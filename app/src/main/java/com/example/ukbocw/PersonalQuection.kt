package com.example.ukbocw

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.example.ukbocw.adapter.QuestionAdapter
import com.example.ukbocw.adapter.QuestionNoAnswerAdapter
import com.example.ukbocw.databinding.ActivityPersonalQuectionBinding
import com.example.ukbocw.model.QuestionOptionType
import com.example.ukbocw.utils.Constant
import com.example.ukbocw.utils.PreferenceHelper
import com.example.ukbocw.utils.generateRandomAlphaNumeric
import com.example.ukbocw.utils.jsonObjectToBase64
import com.example.ukbocw.utils.setDebounceOnClickListener
import com.example.ukbocw.viewModel.LoginViewModel
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@AndroidEntryPoint
class PersonalQuection : AppCompatActivity(), QuestionClickListener {
    lateinit var personalQuectionBinding: ActivityPersonalQuectionBinding
    var position = 0
    lateinit var imageUri: Uri
    private val memberList = JsonObject()
    private var selectedMemberOccupation: String? = null
    private var selectedMemberEducation: String? = null
    lateinit var questionAdapter: QuestionAdapter
    lateinit var questionNoAnswerAdapter: QuestionNoAnswerAdapter
    val editValueField = JsonObject()
    private var otherValue: Boolean? = null
    var optionClick: Boolean = false
    var optionfilled: Boolean = false
    private var creds: BasicAWSCredentials =
        BasicAWSCredentials(Constant.ACCESS_ID, Constant.SECRET_KEY)
    private var s3Client: AmazonS3Client = AmazonS3Client(creds)
    lateinit var file: File
    var documentType: Boolean = false
    private val viewModel by viewModels<LoginViewModel>()
    private lateinit var sharePreference: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        personalQuectionBinding = ActivityPersonalQuectionBinding.inflate(layoutInflater)
        setContentView(personalQuectionBinding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = getColor(R.color.white)

        sharePreference = PreferenceHelper(this)
        personalQuectionBinding.button.setDebounceOnClickListener {
            if (personalQuectionBinding.etAnswer.text.isNotBlank()) {
                optionfilled = true
            }
            if (personalQuectionBinding.etOtherAnswer.text.isNotBlank()) {
                optionfilled = true
            }
            if (optionfilled || optionClick) {
                optionClick = false
                optionfilled = false
                setFormWizzard()
            } else {
                showToast("Please Select/Fill any option")
            }
        }
        personalQuectionBinding.back.setDebounceOnClickListener {
            //setFormReverseWizard()
        }
        personalQuectionBinding.questions.text = getString(R.string.fullname)
        personalQuectionBinding.etAnswer.hint = "Full Name"
        personalQuectionBinding.etAnswer.isVisible = true

        personalQuectionBinding.lFamilyMemberLayout.tvAddMember.setDebounceOnClickListener {
            setFamilyMember()
        }
        personalQuectionBinding.toolbarLayout.backArrow.setDebounceOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        personalQuectionBinding.lDocumentLayout.ivPhotoImage.setDebounceOnClickListener {
            val fileName = generateRandomAlphaNumeric(16)
            documentType = true
            imageUri = createImage("${fileName}.png")!!
            contract.launch(imageUri)
        }
        personalQuectionBinding.lDocumentLayout.ivIdentityImage.setDebounceOnClickListener {
            val fileName = generateRandomAlphaNumeric(16)
            imageUri = createImage("${fileName}.png")!!
            contract.launch(imageUri)
            personalQuectionBinding.button.isVisible = false
            personalQuectionBinding.submit.isVisible = true
        }
        personalQuectionBinding.submit.setDebounceOnClickListener {
            val survey = JsonObject()
            val surveyString = jsonObjectToBase64(editValueField)
            survey.addProperty("survey", surveyString)
            saveSurveyData(
                survey,
                sharePreference.getDataFromPref("userAccessToken").toString()
            )
        }
    }

    private fun saveSurveyData(surveyString: JsonObject, token: String) {

        viewModel.survey(surveyString, token)
        viewModel.surveyResponse.observe(this, {
            var surveyId = it.data.`$oid`
            val intent = Intent(this, Success::class.java).putExtra("surveyId", surveyId)
            startActivity(intent)
            finish()
        })
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun createImage(fileName: String): Uri? {
        val image = File(applicationContext.filesDir, fileName)
        return FileProvider.getUriForFile(
            applicationContext,
            "com.example.ukbocw.FileProvider",
            image
        )

    }

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        val inputStream: InputStream? =
            contentResolver.openInputStream(imageUri)
        file = File.createTempFile("image", imageUri.lastPathSegment)
        val outStream: OutputStream = FileOutputStream(file)
        outStream.write(inputStream!!.readBytes())
        uploadImage()
    }

    private fun uploadImage() {
        val trans = TransferUtility.builder().context(applicationContext).s3Client(s3Client).build()
        val observer: TransferObserver =
            trans.upload(
                Constant.BUCKET_NAME,
                imageUri.lastPathSegment,
                file
            )//manual storage permission
        observer.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    if (documentType) {
                        editValueField.addProperty(
                            "photo",
                            s3Client.getResourceUrl(Constant.BUCKET_NAME, imageUri.lastPathSegment)
                        )
                        documentType = false
                    } else {
                        editValueField.addProperty(
                            "identityProof",
                            s3Client.getResourceUrl(Constant.BUCKET_NAME, imageUri.lastPathSegment)
                        )
                    }
                } else if (state == TransferState.FAILED) {
                    Log.d("msg", "fail")
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                if (bytesCurrent != bytesTotal) {
                    val progress = 100 * bytesCurrent / bytesTotal
                    personalQuectionBinding.lDocumentLayout.progressBar.isVisible = true
                    personalQuectionBinding.lDocumentLayout.progressBar.progress = progress.toInt()
                    showToast("Image Upload Successful")
                } else {
                    personalQuectionBinding.lDocumentLayout.progressBar.isVisible = false
                }
            }

            override fun onError(id: Int, ex: Exception) {
                showToast(ex.message.toString())
            }
        })
    }

    private fun setFamilyMemberDropdown() {
        val memberOccupationAdapter = ArrayAdapter(
            this,
            R.layout.list_layout,
            resources.getStringArray(R.array.memberOccupation)
        )
        personalQuectionBinding.lFamilyMemberLayout.dropdownOccupation.setAdapter(
            memberOccupationAdapter
        )
        personalQuectionBinding.lFamilyMemberLayout.dropdownOccupation.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedMemberOccupation = adapterView.getItemAtPosition(i).toString()
                if (selectedMemberOccupation == "Other") {
                    personalQuectionBinding.lFamilyMemberLayout.etOtherOccupation.visibility =
                        View.VISIBLE
                } else {
                    personalQuectionBinding.lFamilyMemberLayout.etOtherOccupation.visibility =
                        View.GONE
                }
            }

        val memberEducationAdapter = ArrayAdapter(
            this,
            R.layout.list_layout,
            resources.getStringArray(R.array.memberEducation)
        )
        personalQuectionBinding.lFamilyMemberLayout.dropdownEducation.setAdapter(
            memberEducationAdapter
        )
        personalQuectionBinding.lFamilyMemberLayout.dropdownEducation.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedMemberEducation = adapterView.getItemAtPosition(i).toString()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setFormWizzard() {
        when (position) {
            0 -> {
                hideKeyboard()
                personalQuectionBinding.back.isVisible = true
                editValueField.addProperty(
                    "fullName",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                position = 1
                personalQuectionBinding.etAnswer.hint = ""
                personalQuectionBinding.questions.text = getString(R.string.gender)
                val question = QuestionOptionType(
                    arrayListOf("Male", "Female", "Others")
                )
                personalQuectionBinding.rvQuestions.also { recycleView ->
                    recycleView.layoutManager = LinearLayoutManager(this)
                    questionAdapter = QuestionAdapter(question, this, "Gender")
                    recycleView.adapter = questionAdapter
                }
                personalQuectionBinding.rvQuestions.isVisible = true
                personalQuectionBinding.button.text = "Next"
            }

            1 -> {
                position = 2
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text = "3. District"
                personalQuectionBinding.etAnswer.hint = "District"
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.button.text = "Next"
            }

            2 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "district",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "Age"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "(18 –25)",
                            "(26–35)",
                            "(36–45)",
                            "(46–60)",
                            "Above 60"
                        )
                    ), "Age"
                )
                questionAdapter.notifyDataSetChanged()
                position = 3
                personalQuectionBinding.button.text = "Next"
            }

            3 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "4. Area"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Rural",
                            "Urban",
                        )
                    ), "Area"
                )
                questionAdapter.notifyDataSetChanged()
                position = 4
                personalQuectionBinding.button.text = "Next"
            }

            4 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "5. Marital Status"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Married",
                            "Unmarried",
                            "Divorced",
                            "Widow"
                        )
                    ), "MaritalStatus"
                )
                questionAdapter.notifyDataSetChanged()
                position = 5
                personalQuectionBinding.button.text = "Next"
            }

            5 -> {
                position = 6
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text = "6. Mobile Number"
                personalQuectionBinding.etAnswer.hint = "Mobile Number"

                personalQuectionBinding.button.text = "Next"
            }

            6 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "mobile_number",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "7. Education"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Illiterate",
                            "Primary",
                            "Middle",
                            "Matric",
                            "Intermediate",
                            "Graduation",
                            "Above"
                        )
                    ), "Education"
                )
                questionAdapter.notifyDataSetChanged()
                position = 7
                personalQuectionBinding.button.text = "Next"
            }

            7 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "8. Designation"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "ChinaaiMistri",
                            "Majdoor (Helper) ",
                            "Plumber",
                            "Carpenter",
                            "Painter",
                            "Other"
                        )
                    ), "Designation"
                )
                questionAdapter.notifyDataSetChanged()
                position = 8
                personalQuectionBinding.button.text = "Next"
            }

            8 -> {
                if (otherValue == true) {
                    editValueField.addProperty(
                        "designation",
                        personalQuectionBinding.etOtherAnswer.text.toString()
                    )
                    otherValue = null
                    personalQuectionBinding.etOtherAnswer.isVisible = false
                    personalQuectionBinding.etOtherAnswer.text.clear()
                    personalQuectionBinding.tvOtherAnswer.isVisible = false
                }
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "9. Which Social category do you belong?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Scheduled Caste",
                            "Other Backward Classes",
                            "Scheduled Tribe",
                            "General",
                            "Backward Class"
                        )
                    ), "Social_category"
                )
                questionAdapter.notifyDataSetChanged()
                position = 9
                personalQuectionBinding.button.text = "Next"
            }

            9 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "10. Religion"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Hindu",
                            "Muslim",
                            "Christian",
                            "Sikh",
                            "Others"
                        )
                    ), "Religion"
                )
                questionAdapter.notifyDataSetChanged()
                position = 10
                personalQuectionBinding.button.text = "Next"
            }

            10 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "11. Which State do you belong originally?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Uttrakhand",
                            "UttarPradesh",
                            "Bihar",
                            "Bengal",
                            "Jharkhand",
                            "Other",
                        )
                    ), "State"
                )
                questionAdapter.notifyDataSetChanged()
                position = 11
                personalQuectionBinding.button.text = "Next"
            }

            11 -> {
                if (otherValue == true) {
                    editValueField.addProperty(
                        "State",
                        personalQuectionBinding.etOtherAnswer.text.toString()
                    )
                    otherValue = null
                    personalQuectionBinding.etOtherAnswer.isVisible = false
                    personalQuectionBinding.etOtherAnswer.text.clear()
                    personalQuectionBinding.tvOtherAnswer.isVisible = false
                }
                setFamilyMemberDropdown()
                personalQuectionBinding.questions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.clBody.isVisible = false
                personalQuectionBinding.lFamilyMemberLayout.root.isVisible = true
                position = 12
            }

            //need to set family member add section
            12 -> {
                editValueField.addProperty("family_member_details", memberList.toString())
                personalQuectionBinding.questions.isVisible = true
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.clBody.isVisible = true
                personalQuectionBinding.lFamilyMemberLayout.root.isVisible = false
                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Economic Status"
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text = "13. Which Ration Card you hold?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yellow (BPL) ",
                            "Green(APL)",
                            "Pink (AAY)",
                            "Khaki (OPH)",
                            "None",
                        )
                    ), "Ration_Card"
                )
                questionAdapter.notifyDataSetChanged()
                position = 13
                personalQuectionBinding.button.text = "Next"
            }

            13 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "14. What are the other major sources of your family income?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Salary",
                            "Agriculture/ Cattle rearing",
                            "Daily Wage",
                            "Pension",
                            "Other"
                        )
                    ), "major_sources_of_your_family_income"
                )
                questionAdapter.notifyDataSetChanged()
                position = 14
                personalQuectionBinding.button.text = "Next"
            }

            14 -> {
                if (otherValue == true) {
                    editValueField.addProperty(
                        "major_sources_of_your_family_income",
                        personalQuectionBinding.etOtherAnswer.text.toString()
                    )

                    otherValue = null
                    personalQuectionBinding.etOtherAnswer.isVisible = false
                    personalQuectionBinding.etOtherAnswer.text.clear()
                    personalQuectionBinding.tvOtherAnswer.isVisible = false
                }
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "15. Total Monthly Family Income"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Less than 5000",
                            "5000-10000",
                            "11000-15000",
                            "16000 and above"
                        )
                    ), "Monthly_Family_Income"
                )
                questionAdapter.notifyDataSetChanged()
                position = 15
                personalQuectionBinding.button.text = "Next"
            }

            15 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "16. Where do you go for treatment?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Private Hospital",
                            "Government Hospital",
                            "Both",
                        )
                    ), "treatment"
                )
                questionAdapter.notifyDataSetChanged()
                position = 16
                personalQuectionBinding.button.text = "Next"
            }

            16 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "17. Do you possess any land?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "land_possess"
                )
                questionAdapter.notifyDataSetChanged()
                position = 17
                personalQuectionBinding.button.text = "Next"
            }

            17 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "18. Where do you stay?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Own House",
                            "Provided by Employer",
                            "On Rent",
                            "Other",
                        )
                    ), "stay"
                )
                questionAdapter.notifyDataSetChanged()
                position = 18
                personalQuectionBinding.button.text = "Next"
            }

            18 -> {
                if (otherValue == true) {
                    editValueField.addProperty(
                        "stay",
                        personalQuectionBinding.etOtherAnswer.text.toString()
                    )
                    otherValue = null
                    personalQuectionBinding.etOtherAnswer.isVisible = false
                    personalQuectionBinding.etOtherAnswer.text.clear()
                    personalQuectionBinding.tvOtherAnswer.isVisible = false
                }
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "19. Material of the house?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Kachcha",
                            "Pacca",
                            "Semi-Pacca"
                        )
                    ), "house_material"
                )
                questionAdapter.notifyDataSetChanged()
                position = 19
                personalQuectionBinding.button.text = "Next"
            }

            19 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "20. Is there any toilet in the house you live?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "have_washroom"
                )
                questionAdapter.notifyDataSetChanged()
                position = 20
                personalQuectionBinding.button.text = "Next"
            }

            20 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "21. Are you provided with basic amenities at The rented or employer provided house?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "basic_amenities"
                )
                questionAdapter.notifyDataSetChanged()
                position = 21
                personalQuectionBinding.button.text = "Next"
            }

            21 -> {
                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Registration"
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "22. How long have you been the member of UKBOCWW Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "One year",
                            "Two years",
                            "Three years",
                            "More than three years",
                        )
                    ), "no_of_years_member"
                )
                questionAdapter.notifyDataSetChanged()
                position = 22
                personalQuectionBinding.button.text = "Next"
            }

            22 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "23. Have you faced any problem while registering or renewing the membership?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "face_problem_registration"
                )
                questionAdapter.notifyDataSetChanged()
                position = 23
                personalQuectionBinding.button.text = "Next"
            }

            23 -> {
                if (otherValue == true) {
                    editValueField.addProperty(
                        "face_problem_registration_other",
                        personalQuectionBinding.etOtherAnswer.text.toString()
                    )

                    otherValue = null
                    personalQuectionBinding.etOtherAnswer.isVisible = false
                    personalQuectionBinding.etOtherAnswer.text.clear()
                    personalQuectionBinding.tvOtherAnswer.isVisible = false
                }
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "24. Did you face any problem to produce 90 days working certificate?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not aware"
                        )
                    ), "working_certificate"
                )
                questionAdapter.notifyDataSetChanged()
                position = 24
                personalQuectionBinding.button.text = "Next"
            }

            24 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "25. Do you think that the number of days to acquire the working certificate should be reduced?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "No Opinion"
                        )
                    ), "working_certificate_day_reduce"
                )
                questionAdapter.notifyDataSetChanged()
                position = 25
                personalQuectionBinding.button.text = "Next"
            }

            25 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "26. How do you fill the forms?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Myself",
                            "CSC",
                            "Contractor or Employer",
                            "Others",
                        )
                    ), "form_fill"
                )
                questionAdapter.notifyDataSetChanged()
                position = 26
                personalQuectionBinding.button.text = "Next"
            }

            26 -> {
                position = 27
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "27. How much does the CSC center charge you for application?"
                personalQuectionBinding.etAnswer.hint = "Application Charge"
                personalQuectionBinding.button.text = "Next"
            }

            27 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "application_charge",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "28. Do you renew your membership regularly?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "renew_membership_regular"
                )
                questionAdapter.notifyDataSetChanged()
                position = 28
                personalQuectionBinding.button.text = "Next"
            }

            28 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "29. Are you aware about the schemes of UKBOCWWB?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Some of them"
                        )
                    ), "scheme_aware"
                )
                questionAdapter.notifyDataSetChanged()
                position = 29
                personalQuectionBinding.button.text = "Next"
            }

            29 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "30. Where you got the information about the UKBOCWWB Schemes?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Media",
                            "Friends",
                            "Co-Workers",
                            "Union",
                            "Others",
                        )
                    ), "know_about_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 30
                personalQuectionBinding.button.text = "Next"
            }

            30 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "31. Have you ever informed about the scheme by the UKBOCWWB board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                        )
                    ), "informed_about_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 31
                personalQuectionBinding.button.text = "Next"
            }

            31 -> {
                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Maternity Benefit scheme"
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "32. Are you aware about the Maternity Benefit scheme of UKBOCWWB?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                        )
                    ), "aware_maternity_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 32
                personalQuectionBinding.button.text = "Next"
            }

            32 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "33. Have you availed the benefits under the Maternity Benefit scheme?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "availed_maternity_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 33
                personalQuectionBinding.button.text = "Next"
            }

            33 -> {
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "34. How is the fund utilized?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Food and Nutrition",
                            "Medicine ",
                            "Facilities",
                            "Other"
                        )
                    ), "fund_utilized_maternity_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 34
                personalQuectionBinding.button.text = "Next"
            }

            34 -> {
                if (otherValue == true) {
                    editValueField.addProperty(
                        "fund_utilized_maternity_scheme_other",
                        personalQuectionBinding.etOtherAnswer.text.toString()
                    )
                    otherValue = null
                    personalQuectionBinding.etOtherAnswer.isVisible = false
                    personalQuectionBinding.etOtherAnswer.text.clear()
                    personalQuectionBinding.tvOtherAnswer.isVisible = false
                }
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "35. Was the financial assistance helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient",
                            "Partially"
                        )
                    ), "financial_assistance_maternity_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 35
                personalQuectionBinding.button.text = "Next"
            }

            35 -> {
                position = 36
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "36. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            36 -> {
                position = 37
                editValueField.addProperty(
                    "scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "37. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenges faced to avail the scheme"
                personalQuectionBinding.button.text = "Next"
            }

            37 -> {
                editValueField.addProperty(
                    "challenges_face_opinion",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text = "60 Years pension scheme"
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "38. Are you aware about the 60 Years pension scheme of UKBOCWWB?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                        )
                    ), "aware_sixty_year_pension"
                )
                questionAdapter.notifyDataSetChanged()
                position = 38
                personalQuectionBinding.button.text = "Next"
            }

            38 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "39. Are you getting the Pension under the scheme?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "Applied, but not received",
                        )
                    ), "get_pension_under_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 39
                personalQuectionBinding.button.text = "Next"
            }

            39 -> {
                editValueField.addProperty(
                    "pension_applied_but_not_received",
                    personalQuectionBinding.etOtherAnswer.text.toString()
                )
                personalQuectionBinding.etOtherAnswer.text.clear()
                personalQuectionBinding.etOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "40. If yes, are you getting the Pension regularly under this scheme?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                        )
                    ), "get_pension_regular_under_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 40
                personalQuectionBinding.button.text = "Next"
            }

            40 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "41. Where do you utilize the fund?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Food",
                            "Health",
                            "Children’s education",
                            "Other"

                        )
                    ), "pension_fund_utilized"
                )
                questionAdapter.notifyDataSetChanged()
                position = 41
                personalQuectionBinding.button.text = "Next"
            }

            41 -> {
                if (otherValue == true) {
                    editValueField.addProperty(
                        "pension_fund_utilized_other",
                        personalQuectionBinding.etOtherAnswer.text.toString()
                    )
                    otherValue = null
                    personalQuectionBinding.etOtherAnswer.isVisible = false
                    personalQuectionBinding.etOtherAnswer.text.clear()
                    personalQuectionBinding.tvOtherAnswer.isVisible = false
                }
                personalQuectionBinding.questions.text =
                    "42. Was the financial assistance helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient ",
                            "Partially"
                        )
                    ), "pension_fund_utilized"
                )
                questionAdapter.notifyDataSetChanged()
                position = 42
                personalQuectionBinding.button.text = "Next"
            }

            42 -> {
                position = 43
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "43. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            43 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "pension_scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 44
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "44. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            44 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "pension_scheme_challenge_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Financial Assistance for Education"
                personalQuectionBinding.questions.text =
                    "45. The UKBOCW Welfare Board provides Financial Assistance for Education to the registered workers children. Are you aware about the Education Assistance?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_financial_assistance_education"
                )
                questionAdapter.notifyDataSetChanged()
                position = 45
                personalQuectionBinding.button.text = "Next"
            }

            45 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "46. Have you got any financial assistance for your children’s education?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "get_financial_assistance_education"
                )
                questionAdapter.notifyDataSetChanged()
                position = 46
                personalQuectionBinding.button.text = "Next"
            }

            46 -> {
                personalQuectionBinding.questions.text =
                    "47. If availed, for which are the following:"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Class 1 to Class 5 (1800/year)",
                            "Class 6 to Class 10 (2400/year)",
                            "Class 11 to Class 12 (3000/year)",
                            "Bachelor’s degree or equivalent (10000/year)",
                            "Meritorious Children (up to 12th)",
                            "Competitive Exams",
                        )
                    ), "availed_financial_assistance_education"
                )
                questionAdapter.notifyDataSetChanged()
                position = 47
                personalQuectionBinding.button.text = "Next"
            }

            47 -> {
                personalQuectionBinding.questions.text =
                    "48. How many times you got the benefit of this scheme?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "One time",
                            "Two Times",
                            "Three Times",
                            "Four Times",
                        )
                    ), "times_financial_assistance_education"
                )
                questionAdapter.notifyDataSetChanged()
                position = 48
                personalQuectionBinding.button.text = "Next"
            }

            48 -> {

                personalQuectionBinding.questions.text =
                    "49. Where is the fund utilized?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Fees",
                            "Study Material/ Gadgets",
                            "Commuting to the Institutions",
                            "Other",
                        )
                    ), "fund_utilized_financial_assistance_education"
                )
                questionAdapter.notifyDataSetChanged()
                position = 49
                personalQuectionBinding.button.text = "Next"
            }

            49 -> {
                if (otherValue == true) {
                    editValueField.addProperty(
                        "fund_utilized_financial_assistance_education_other",
                        personalQuectionBinding.etOtherAnswer.text.toString()
                    )
                    otherValue = null
                    personalQuectionBinding.etOtherAnswer.isVisible = false
                    personalQuectionBinding.etOtherAnswer.text.clear()
                    personalQuectionBinding.tvOtherAnswer.isVisible = false
                }
                personalQuectionBinding.questions.text =
                    "50. Was the financial assistance helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient ",
                            "Partially",
                        )
                    ), "is_helpful_financial_assistance_education"
                )
                questionAdapter.notifyDataSetChanged()
                position = 50
                personalQuectionBinding.button.text = "Next"
            }

            50 -> {
                position = 51
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "51. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            51 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "financial_assistance_education_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 52
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "52. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            52 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "financial_assistance_education_challenge_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Medical/ Health Insurance"
                personalQuectionBinding.questions.text =
                    "53. The UKBOCW Welfare Board gives assistance to Medical/ Health Insurance (Medical assistance under Rashtriya Swasthya Bima Yojna) to the construction workers. Are you aware about it?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_health_insurance"
                )
                questionAdapter.notifyDataSetChanged()
                position = 53
                personalQuectionBinding.button.text = "Next"
            }

            53 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "54. Did you avail this scheme provided by the UKBOCWWB?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "availed_health_insurance"
                )
                questionAdapter.notifyDataSetChanged()
                position = 54
                personalQuectionBinding.button.text = "Next"
            }

            54 -> {
                position = 55
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "55. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            55 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "health_insurance_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 56
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "56. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            56 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "health_insurance_challenge_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Home Loan"
                personalQuectionBinding.questions.text =
                    "57. The UK Building and other Construction Workers Welfare Board provide Home Loan. Are you aware about it?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_home_loan"
                )
                questionAdapter.notifyDataSetChanged()
                position = 57
                personalQuectionBinding.button.text = "Next"
            }

            57 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "58. Did you get any assistance for the Purchase/ Built of House under Housing Scheme?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "assistance_home_loan"
                )
                questionAdapter.notifyDataSetChanged()
                position = 58
                personalQuectionBinding.button.text = "Next"
            }

            58 -> {
                personalQuectionBinding.questions.text =
                    "59. If Yes, have you built/ purchased the house under Housing scheme?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Under construction"
                        )
                    ), "built_house_under_home_loan"
                )
                questionAdapter.notifyDataSetChanged()
                position = 59
                personalQuectionBinding.button.text = "Next"
            }

            59 -> {
                personalQuectionBinding.questions.text =
                    "60. Was the financial assistance helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient",
                            "Partially"
                        )
                    ), "meet_the_need_home_loan"
                )
                questionAdapter.notifyDataSetChanged()
                position = 60
                personalQuectionBinding.button.text = "Next"
            }

            60 -> {
                personalQuectionBinding.questions.text =
                    "61. Does this scheme being loan assistance deter you from availing it?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "availing_home_loan"
                )
                questionAdapter.notifyDataSetChanged()
                position = 61
                personalQuectionBinding.button.text = "Next"
            }

            61 -> {
                position = 62
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "62. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            62 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "home_loan_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 63
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "63. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            63 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "home_loan_challenge_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Disability Pension Scheme"
                personalQuectionBinding.questions.text =
                    "64. Are you aware about Disability Pension Scheme of UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_disability_pension_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 64
                personalQuectionBinding.button.text = "Next"
            }

            64 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "65. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "assistance_disability_pension_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 65
                personalQuectionBinding.button.text = "Next"
            }

            65 -> {
                personalQuectionBinding.questions.text =
                    "66. Was the financial assistance helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient",
                            "Partially"
                        )
                    ), "meet_disability_pension_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 66
                personalQuectionBinding.button.text = "Next"
            }

            66 -> {
                position = 67
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "67.What are the impacts of this scheme on your condition?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            67 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "disability_pension_scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 68
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "68. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            68 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "disability_pension_scheme_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Financial Assistance due to Death"
                personalQuectionBinding.questions.text =
                    "69. Are you aware about Financial Assistance due to Death (Rs. 20,000 for Natural Death & Rs.50,000 for Accidental death of registered construction worker) by UKBOCWWB?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_death_financial_help"
                )
                questionAdapter.notifyDataSetChanged()
                position = 69
                personalQuectionBinding.button.text = "Next"
            }

            69 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "70. Have you availed the benefits under this scheme?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "availed_death_financial_help"
                )
                questionAdapter.notifyDataSetChanged()
                position = 70
                personalQuectionBinding.button.text = "Next"
            }

            70 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "71. How many times you availed the benefits?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "One Time",
                            "Two Times",
                            "Three times",
                            "More than three times"
                        )
                    ), "times_availed_death_financial_help"
                )
                questionAdapter.notifyDataSetChanged()
                position = 71
                personalQuectionBinding.button.text = "Next"
            }

            71 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "72. Was the financial assistance helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient",
                            "Partially",
                        )
                    ), "meet_need_death_financial_help"
                )
                questionAdapter.notifyDataSetChanged()
                position = 72
                personalQuectionBinding.button.text = "Next"
            }

            72 -> {
                position = 73
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "73. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            73 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "death_financial_help_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 74
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "74. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            74 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "death_financial_help_challenge_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Funeral Assistance"
                personalQuectionBinding.questions.text =
                    "75. The UKBOCWWB provides Funeral Assistance (Rs 10,000). Are you aware about the Assistance Scheme?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_funeral_assistance"
                )
                questionAdapter.notifyDataSetChanged()
                position = 75
                personalQuectionBinding.button.text = "Next"
            }

            75 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "76. Does the lengthy process and the benefit be given only once a year deter you from availing the benefit?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                        )
                    ), "availed_funeral_assistance"
                )
                questionAdapter.notifyDataSetChanged()
                position = 76
                personalQuectionBinding.button.text = "Next"
            }

            76 -> {
                position = 77
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "77. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            77 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "death_financial_help_impacts",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Tool Kit Assistance"
                personalQuectionBinding.questions.text =
                    "78. Are you aware about Tool Kit Assistance by UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_tool_kit"
                )
                questionAdapter.notifyDataSetChanged()
                position = 78
                personalQuectionBinding.button.text = "Next"
            }

            78 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "79. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                        )
                    ), "availed_tool_kit"
                )
                questionAdapter.notifyDataSetChanged()
                position = 79
                personalQuectionBinding.button.text = "Next"
            }

            79 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "80. Was the financial assistance helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient",
                            "Partially"
                        )
                    ), "meet_need_tool_kit"
                )
                questionAdapter.notifyDataSetChanged()
                position = 80
                personalQuectionBinding.button.text = "Next"
            }

            80 -> {
                position = 81
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "81. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            81 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "tool_kit_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 83
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "82. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            83 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "tool_kit_challenge_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Bicycle Scheme"
                personalQuectionBinding.questions.text =
                    "83. Are you aware about Bicycle Scheme (Construction workers of Plain area registered workers) by UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_bicycle_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 84
                personalQuectionBinding.button.text = "Next"
            }

            84 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "84. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "availed_bicycle_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 85
                personalQuectionBinding.button.text = "Next"
            }

            85 -> {
                position = 86
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "85. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            86 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "bicycle_scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 87
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "86. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            87 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "tool_kit_challenge_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Sewing Machine Scheme"
                personalQuectionBinding.questions.text =
                    "87. Are you aware about Sewing Machine Scheme (Sewing machine to construction workers of mountainous areas or their dependents) by UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_sewing_machine_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 88
                personalQuectionBinding.button.text = "Next"
            }

            88 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "88. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "avail_sewing_machine_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 89
                personalQuectionBinding.button.text = "Next"
            }

            89 -> {
                position = 90
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "89. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            90 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "sewing_machine_scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 91
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "90. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            91 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "aware_sewing_machine_scheme_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Sanitary Napkin Scheme"
                personalQuectionBinding.questions.text =
                    "91. Are you aware about Sanitary Napkin Scheme (Sanitary napkins to the registered female workers or the daughters of registered workers) by UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_sanitary_napkins_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 92
                personalQuectionBinding.button.text = "Next"
            }

            92 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "92. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "avail_sanitary_napkins_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 93
                personalQuectionBinding.button.text = "Next"
            }

            93 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "93. Was the assistance helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient",
                            "Partially"
                        )
                    ), "needs_sanitary_napkins_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 94
                personalQuectionBinding.button.text = "Next"
            }

            94 -> {
                position = 95
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "94. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            95 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "sanitary_napkins_scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 96
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "95. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            96 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "sanitary_napkins_scheme_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Skill Training Scheme"
                personalQuectionBinding.questions.text =
                    "96. Are you aware about Skill Training (collaboration with NIESBUD to train & form SHG groups of female members of registered workers under UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_skill_training_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 97
                personalQuectionBinding.button.text = "Next"
            }

            97 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "97. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "avail_skill_training_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 98
                personalQuectionBinding.button.text = "Next"
            }

            98 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "98. Was the Skill Training helpful?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Sufficient",
                            "Not Sufficient ",
                            "Partially"
                        )
                    ), "helpful_skill_training_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 99
                personalQuectionBinding.button.text = "Next"
            }

            99 -> {
                position = 100
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "99. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            100 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "skill_training_scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 101
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "100. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            101 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "skill_training_scheme_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Solar Light Scheme"
                personalQuectionBinding.questions.text =
                    "101. Are you aware about Solar Light Scheme (once in lifetime) by UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_solar_light_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 102
                personalQuectionBinding.button.text = "Next"
            }

            102 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "102. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "avail_solar_light_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 103
                personalQuectionBinding.button.text = "Next"
            }

            103 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "103. Was the Scheme helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Partially",
                            "Not Sufficient ",
                            "Sufficient"
                        )
                    ), "helpful_solar_light_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 104
                personalQuectionBinding.button.text = "Next"
            }

            104 -> {
                position = 105
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "104. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            105 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "solar_light_scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 106
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "105. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            106 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "solar_light_scheme_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Umbrella Scheme"
                personalQuectionBinding.questions.text =
                    "106. Are you aware about Umbrella Scheme (once in a lifetime) by UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_Umbrella_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 107
                personalQuectionBinding.button.text = "Next"
            }

            107 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "107. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "avail_Umbrella_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 108
                personalQuectionBinding.button.text = "Next"
            }

            108 -> {
                position = 109
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "108. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            109 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "Umbrella_scheme_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 110
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "109. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            110 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "solar_light_scheme_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Skill Up gradation Scheme"
                personalQuectionBinding.questions.text =
                    "110. Are you aware about Skill Up gradation by UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_skill_up_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 111
                personalQuectionBinding.button.text = "Next"
            }

            111 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "111. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "avail_skill_up_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 112
                personalQuectionBinding.button.text = "Next"
            }

            112 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "112. Was the Scheme helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Partially",
                            "Not Sufficient ",
                            "Sufficient"
                        )
                    ), "helpful_skill_up_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 113
                personalQuectionBinding.button.text = "Next"
            }

            113 -> {
                position = 114
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "113. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            114 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "skill_up_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 115
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "114. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            115 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "skill_up_scheme_faced",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.toolbarLayout.tvToolbar.text =
                    "Construction of Toilets"
                personalQuectionBinding.questions.text =
                    "115. Are you aware about Construction of Toilets (Rs. 12,000 for construction of toilets to registered eligible construction workers) by UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No"
                        )
                    ), "aware_washroom_construction_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 116
                personalQuectionBinding.button.text = "Next"
            }

            116 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "116. Have you got benefits of the scheme from UKBOCW Welfare Board?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Yes",
                            "No",
                            "Not Eligible"
                        )
                    ), "avail_washroom_construction_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 117
                personalQuectionBinding.button.text = "Next"
            }

            117 -> {
                personalQuectionBinding.rvOtherAnswer.isVisible = false
                personalQuectionBinding.tvOtherAnswer.isVisible = false
                personalQuectionBinding.questions.text =
                    "117. Was the Scheme helpful to meet the needs?"
                personalQuectionBinding.rvQuestions.isVisible = true
                questionAdapter.addOtherQuestion(
                    QuestionOptionType(
                        arrayListOf(
                            "Partially",
                            "Not Sufficient ",
                            "Sufficient"
                        )
                    ), "helpful_washroom_construction_scheme"
                )
                questionAdapter.notifyDataSetChanged()
                position = 118
                personalQuectionBinding.button.text = "Next"
            }

            118 -> {
                position = 119
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.rvQuestions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "118. What are the impacts of this scheme?"
                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            119 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "washroom_construction_impact",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                position = 120
                personalQuectionBinding.etAnswer.text.clear()
                personalQuectionBinding.etAnswer.isVisible = true
                personalQuectionBinding.questions.text =
                    "119. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
                personalQuectionBinding.button.text = "Next"
            }

            120 -> {
                hideKeyboard()
                editValueField.addProperty(
                    "washroom_construction_challenge_face",
                    personalQuectionBinding.etAnswer.text.toString()
                )
                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Document Upload"
                personalQuectionBinding.questions.isVisible = false
                personalQuectionBinding.etAnswer.isVisible = false
                personalQuectionBinding.clBody.isVisible = false
                personalQuectionBinding.lDocumentLayout.root.isVisible = true
            }

            else -> {

            }
        }
    }


    private fun setFamilyMember() {

        memberList.addProperty(
            memberList.size().toString(),
            listOf(
                personalQuectionBinding.lFamilyMemberLayout.etFullName.text.toString(),
                selectedMemberOccupation,
                personalQuectionBinding.lFamilyMemberLayout.etOtherOccupation.text.toString(),
                selectedMemberOccupation
            ).toString()
        )
        optionClick = true
        personalQuectionBinding.lFamilyMemberLayout.etFullName.text.clear()
    }

    fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun getOptionItemClicked(option: String, question: String) {
        optionClick = true
        if (option == "Other") {
            otherValue = true
            personalQuectionBinding.tvOtherAnswer.text = "If Select Other?"
            personalQuectionBinding.etOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
        } else if (option == "Yes" && question == "face_problem_registration") {
            editValueField.addProperty(question, option)
            otherValue = true
            personalQuectionBinding.tvOtherAnswer.text =
                "Explain Your Problem while you registering or renewing the membership."
            personalQuectionBinding.etOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true

        } else if (option == "No" && question == "renew_membership_regular") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Not Aware ",
                            "Lack of Documents",
                            "Lack of Cooperation from CSC",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "get_pension_under_scheme" && option != "Yes") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If applied but not availed, why?"
            personalQuectionBinding.etOtherAnswer.isVisible = true
            personalQuectionBinding.etOtherAnswer.hint =
                "Give reason for not availed your pension after applied"
        } else if (question == "aware_maternity_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "Applied but not availed",
                            "Insufficient Documents",
                            "No minimum 1-year regular Membership",
                            "Already availed Maternity benefit",
                            "Lack of Information (criteria)",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_sixty_year_pension" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "Applied but not availed",
                            "Insufficient Documents",
                            "No minimum 1-year regular Membership",
                            "Already availed Pension benefit",
                            "Lack of Information (criteria)",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_financial_assistance_education" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Students failed to get promoted",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Applied but not availed",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_health_insurance" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_home_loan" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 5-year registration",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Avail Housing benefit under some other scheme",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_disability_pension_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_death_financial_help" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_tool_kit" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_bicycle_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_sewing_machine_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_sanitary_napkins_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_skill_training_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_solar_light_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_Umbrella_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else if (question == "aware_skill_up_scheme" && option == "No") {
            editValueField.addProperty(question, option)
            personalQuectionBinding.rvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.isVisible = true
            personalQuectionBinding.tvOtherAnswer.text = "If no why ?"
            var questionNo = "${question}_no"
            personalQuectionBinding.rvOtherAnswer.also { recycleView ->
                recycleView.layoutManager = LinearLayoutManager(this)
                questionNoAnswerAdapter = QuestionNoAnswerAdapter(
                    QuestionOptionType(
                        arrayListOf(
                            "Incomplete application",
                            "No minimum 1-year regular Membership",
                            "Insufficient Documents",
                            "Applied but not availed",
                            "Lack of Information (criteria) ",
                            "Non-cooperation from officers",
                            "Others",
                        )
                    ), this, questionNo
                )
                recycleView.adapter = questionNoAnswerAdapter
            }
        } else {
            editValueField.addProperty(question, option)
        }
        //Log.d("answer", "$editValueField")
    }


//    private fun setFormReverseWizard() {
//        when (position) {
//            0 -> {
//                personalQuectionBinding.questions.text = getString(R.string.fullname)
//                personalQuectionBinding.etAnswer.hint = "Full Name"
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.rvQuestions.isVisible = false
//
//            }
//
//            1 -> {
//                hideKeyboard()
//                personalQuectionBinding.back.isVisible = true
//                editValueField["fullName"] = personalQuectionBinding.etAnswer.text.toString()
//                editValueField.addProperty("fullName",personalQuectionBinding.etAnswer.text.toString())
//                personalQuectionBinding.etAnswer.isVisible = false
//                position = 0
//                personalQuectionBinding.etAnswer.hint = ""
//                personalQuectionBinding.questions.text = getString(R.string.gender)
//                val question = QuestionOptionType(
//                    arrayListOf("Male", "Female", "Others")
//                )
//                personalQuectionBinding.rvQuestions.also { recycleView ->
//                    recycleView.layoutManager = LinearLayoutManager(this)
//                    questionAdapter = QuestionAdapter(question, this, "Gender")
//                    recycleView.adapter = questionAdapter
//                }
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            2 -> {
//                position = 1
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text = "3. District"
//                personalQuectionBinding.etAnswer.hint = "District"
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            3 -> {
//                hideKeyboard()
//                editValueField["district"] = personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "Age"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "(18 –25)",
//                            "(26–35)",
//                            "(36–45)",
//                            "(46–60)",
//                            "Above 60"
//                        )
//                    ), "Age"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 2
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            4 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "4. Area"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Rural",
//                            "Urban",
//                        )
//                    ), "Area"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 3
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            5 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "5. Marital Status"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Married",
//                            "Unmarried",
//                            "Divorced",
//                            "Widow"
//                        )
//                    ), "MaritalStatus"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 4
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            5 -> {
//                position = 6
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text = "6. Mobile Number"
//                personalQuectionBinding.etAnswer.hint = "Mobile Number"
//
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            6 -> {
//                hideKeyboard()
//                editValueField["mobile_number"] = personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "7. Education"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Illiterate",
//                            "Primary",
//                            "Middle",
//                            "Matric",
//                            "Intermediate",
//                            "Graduation",
//                            "Above"
//                        )
//                    ), "Education"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 7
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            7 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "8. Designation"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "ChinaaiMistri",
//                            "Majdoor (Helper) ",
//                            "Plumber",
//                            "Carpenter",
//                            "Painter",
//                            "Other"
//                        )
//                    ), "Designation"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 8
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            8 -> {
//                if (otherValue == true) {
//                    editValueField["designation"] =
//                        personalQuectionBinding.etOtherAnswer.text.toString()
//                    otherValue = null
//                    personalQuectionBinding.etOtherAnswer.isVisible = false
//                    personalQuectionBinding.etOtherAnswer.text.clear()
//                    personalQuectionBinding.tvOtherAnswer.isVisible = false
//                }
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "9. Which Social category do you belong?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Scheduled Caste",
//                            "Other Backward Classes",
//                            "Scheduled Tribe",
//                            "General",
//                            "Backward Class"
//                        )
//                    ), "Social_category"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 9
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            9 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "10. Religion"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Hindu",
//                            "Muslim",
//                            "Christian",
//                            "Sikh",
//                            "Others"
//                        )
//                    ), "Religion"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 10
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            10 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "11. Which State do you belong originally?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Uttrakhand",
//                            "UttarPradesh",
//                            "Bihar",
//                            "Bengal",
//                            "Jharkhand",
//                            "Other",
//                        )
//                    ), "State"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 11
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            11 -> {
//                if (otherValue == true) {
//                    editValueField["State"] =
//                        personalQuectionBinding.etOtherAnswer.text.toString()
//                    otherValue = null
//                    personalQuectionBinding.etOtherAnswer.isVisible = false
//                    personalQuectionBinding.etOtherAnswer.text.clear()
//                    personalQuectionBinding.tvOtherAnswer.isVisible = false
//                }
//                setFamilyMemberDropdown()
//                personalQuectionBinding.questions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.clBody.isVisible = false
//                personalQuectionBinding.lFamilyMemberLayout.root.isVisible = true
//                position = 12
//            }
//
//            //need to set family member add section
//            12 -> {
//                editValueField["family_member_details"] = memberList.toString()
//                personalQuectionBinding.questions.isVisible = true
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.clBody.isVisible = true
//                personalQuectionBinding.lFamilyMemberLayout.root.isVisible = false
//                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Economic Status"
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text = "13. Which Ration Card you hold?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yellow (BPL) ",
//                            "Green(APL)",
//                            "Pink (AAY)",
//                            "Khaki (OPH)",
//                            "None",
//                        )
//                    ), "Ration_Card"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 13
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            13 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "14. What are the other major sources of your family income?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Salary",
//                            "Agriculture/ Cattle rearing",
//                            "Daily Wage",
//                            "Pension",
//                            "Other"
//                        )
//                    ), "major_sources_of_your_family_income"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 14
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            14 -> {
//                if (otherValue == true) {
//                    editValueField["major_sources_of_your_family_income"] =
//                        personalQuectionBinding.etOtherAnswer.text.toString()
//                    otherValue = null
//                    personalQuectionBinding.etOtherAnswer.isVisible = false
//                    personalQuectionBinding.etOtherAnswer.text.clear()
//                    personalQuectionBinding.tvOtherAnswer.isVisible = false
//                }
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "15. Total Monthly Family Income"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Less than 5000",
//                            "5000-10000",
//                            "11000-15000",
//                            "16000 and above"
//                        )
//                    ), "Monthly_Family_Income"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 15
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            15 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "16. Where do you go for treatment?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Private Hospital",
//                            "Government Hospital",
//                            "Both",
//                        )
//                    ), "treatment"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 16
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            16 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "17. Do you possess any land?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "land_possess"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 17
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            17 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "18. Where do you stay?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Own House",
//                            "Provided by Employer",
//                            "On Rent",
//                            "Other",
//                        )
//                    ), "stay"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 18
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            18 -> {
//                if (otherValue == true) {
//                    editValueField["stay"] =
//                        personalQuectionBinding.etOtherAnswer.text.toString()
//                    otherValue = null
//                    personalQuectionBinding.etOtherAnswer.isVisible = false
//                    personalQuectionBinding.etOtherAnswer.text.clear()
//                    personalQuectionBinding.tvOtherAnswer.isVisible = false
//                }
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "19. Material of the house?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Kachcha",
//                            "Pacca",
//                            "Semi-Pacca"
//                        )
//                    ), "house_material"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 19
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            19 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "20. Is there any toilet in the house you live?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "have_washroom"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 20
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            20 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "21. Are you provided with basic amenities at The rented or employer provided house?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "basic_amenities"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 21
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            21 -> {
//                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Registration"
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "22. How long have you been the member of UKBOCWW Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "One year",
//                            "Two years",
//                            "Three years",
//                            "More than three years",
//                        )
//                    ), "no_of_years_member"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 22
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            22 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "23. Have you faced any problem while registering or renewing the membership?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "face_problem_registration"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 23
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            23 -> {
//                if (otherValue == true) {
//                    editValueField["face_problem_registration_other"] =
//                        personalQuectionBinding.etOtherAnswer.text.toString()
//                    otherValue = null
//                    personalQuectionBinding.etOtherAnswer.isVisible = false
//                    personalQuectionBinding.etOtherAnswer.text.clear()
//                    personalQuectionBinding.tvOtherAnswer.isVisible = false
//                }
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "24. Did you face any problem to produce 90 days working certificate?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not aware"
//                        )
//                    ), "working_certificate"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 24
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            24 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "25. Do you think that the number of days to acquire the working certificate should be reduced?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "No Opinion"
//                        )
//                    ), "working_certificate_day_reduce"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 25
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            25 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "26. How do you fill the forms?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Myself",
//                            "CSC",
//                            "Contractor or Employer",
//                            "Others",
//                        )
//                    ), "form_fill"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 26
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            26 -> {
//                position = 27
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "27. How much does the CSC center charge you for application?"
//                personalQuectionBinding.etAnswer.hint = "Application Charge"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            27 -> {
//                hideKeyboard()
//                editValueField["application_charge"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "28. Do you renew your membership regularly?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "renew_membership_regular"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 28
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            28 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "29. Are you aware about the schemes of UKBOCWWB?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Some of them"
//                        )
//                    ), "scheme_aware"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 29
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            29 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "30. Where you got the information about the UKBOCWWB Schemes?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Media",
//                            "Friends",
//                            "Co-Workers",
//                            "Union",
//                            "Others",
//                        )
//                    ), "know_about_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 30
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            30 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "31. Have you ever informed about the scheme by the UKBOCWWB board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                        )
//                    ), "informed_about_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 31
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            31 -> {
//                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Maternity Benefit scheme"
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "32. Are you aware about the Maternity Benefit scheme of UKBOCWWB?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                        )
//                    ), "aware_maternity_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 32
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            32 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "33. Have you availed the benefits under the Maternity Benefit scheme?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "availed_maternity_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 33
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            33 -> {
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "34. How is the fund utilized?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Food and Nutrition",
//                            "Medicine ",
//                            "Facilities",
//                            "Other"
//                        )
//                    ), "fund_utilized_maternity_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 34
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            34 -> {
//                if (otherValue == true) {
//                    editValueField["fund_utilized_maternity_scheme_other"] =
//                        personalQuectionBinding.etOtherAnswer.text.toString()
//                    otherValue = null
//                    personalQuectionBinding.etOtherAnswer.isVisible = false
//                    personalQuectionBinding.etOtherAnswer.text.clear()
//                    personalQuectionBinding.tvOtherAnswer.isVisible = false
//                }
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "35. Was the financial assistance helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient",
//                            "Partially"
//                        )
//                    ), "financial_assistance_maternity_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 35
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            35 -> {
//                position = 36
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "36. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            36 -> {
//                position = 37
//                editValueField["scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "37. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenges faced to avail the scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            37 -> {
//                editValueField["challenges_face_opinion"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text = "60 Years pension scheme"
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "38. Are you aware about the 60 Years pension scheme of UKBOCWWB?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                        )
//                    ), "aware_sixty_year_pension"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 38
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            38 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "39. Are you getting the Pension under the scheme?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "Applied, but not received",
//                        )
//                    ), "get_pension_under_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 39
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            39 -> {
//                editValueField["pension_applied_but_not_received"] =
//                    personalQuectionBinding.etOtherAnswer.text.toString()
//                personalQuectionBinding.etOtherAnswer.text.clear()
//                personalQuectionBinding.etOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "40. If yes, are you getting the Pension regularly under this scheme?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                        )
//                    ), "get_pension_regular_under_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 40
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            40 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "41. Where do you utilize the fund?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Food",
//                            "Health",
//                            "Children’s education",
//                            "Other"
//
//                        )
//                    ), "pension_fund_utilized"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 41
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            41 -> {
//                if (otherValue == true) {
//                    editValueField["pension_fund_utilized_other"] =
//                        personalQuectionBinding.etOtherAnswer.text.toString()
//                    otherValue = null
//                    personalQuectionBinding.etOtherAnswer.isVisible = false
//                    personalQuectionBinding.etOtherAnswer.text.clear()
//                    personalQuectionBinding.tvOtherAnswer.isVisible = false
//                }
//                personalQuectionBinding.questions.text =
//                    "42. Was the financial assistance helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient ",
//                            "Partially"
//                        )
//                    ), "pension_fund_utilized"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 42
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            42 -> {
//                position = 43
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "43. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            43 -> {
//                hideKeyboard()
//                editValueField["pension_scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 44
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "44. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            44 -> {
//                hideKeyboard()
//                editValueField["pension_scheme_challenge_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Financial Assistance for Education"
//                personalQuectionBinding.questions.text =
//                    "45. The UKBOCW Welfare Board provides Financial Assistance for Education to the registered workers children. Are you aware about the Education Assistance?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_financial_assistance_education"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 45
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            45 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "46. Have you got any financial assistance for your children’s education?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "get_financial_assistance_education"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 46
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            46 -> {
//                personalQuectionBinding.questions.text =
//                    "47. If availed, for which are the following:"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Class 1 to Class 5 (1800/year)",
//                            "Class 6 to Class 10 (2400/year)",
//                            "Class 11 to Class 12 (3000/year)",
//                            "Bachelor’s degree or equivalent (10000/year)",
//                            "Meritorious Children (up to 12th)",
//                            "Competitive Exams",
//                        )
//                    ), "availed_financial_assistance_education"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 47
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            47 -> {
//                personalQuectionBinding.questions.text =
//                    "48. How many times you got the benefit of this scheme?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "One time",
//                            "Two Times",
//                            "Three Times",
//                            "Four Times",
//                        )
//                    ), "times_financial_assistance_education"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 48
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            48 -> {
//
//                personalQuectionBinding.questions.text =
//                    "49. Where is the fund utilized?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Fees",
//                            "Study Material/ Gadgets",
//                            "Commuting to the Institutions",
//                            "Other",
//                        )
//                    ), "fund_utilized_financial_assistance_education"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 49
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            49 -> {
//                if (otherValue == true) {
//                    editValueField["fund_utilized_financial_assistance_education_other"] =
//                        personalQuectionBinding.etOtherAnswer.text.toString()
//                    otherValue = null
//                    personalQuectionBinding.etOtherAnswer.isVisible = false
//                    personalQuectionBinding.etOtherAnswer.text.clear()
//                    personalQuectionBinding.tvOtherAnswer.isVisible = false
//                }
//                personalQuectionBinding.questions.text =
//                    "50. Was the financial assistance helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient ",
//                            "Partially",
//                        )
//                    ), "is_helpful_financial_assistance_education"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 50
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            50 -> {
//                position = 51
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "51. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            51 -> {
//                hideKeyboard()
//                editValueField["financial_assistance_education_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 52
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "52. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            52 -> {
//                hideKeyboard()
//                editValueField["financial_assistance_education_challenge_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Medical/ Health Insurance"
//                personalQuectionBinding.questions.text =
//                    "53. The UKBOCW Welfare Board gives assistance to Medical/ Health Insurance (Medical assistance under Rashtriya Swasthya Bima Yojna) to the construction workers. Are you aware about it?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_health_insurance"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 53
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            53 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "54. Did you avail this scheme provided by the UKBOCWWB?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "availed_health_insurance"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 54
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            54 -> {
//                position = 55
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "55. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            55 -> {
//                hideKeyboard()
//                editValueField["health_insurance_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 56
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "56. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            56 -> {
//                hideKeyboard()
//                editValueField["health_insurance_challenge_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Home Loan"
//                personalQuectionBinding.questions.text =
//                    "57. The UK Building and other Construction Workers Welfare Board provide Home Loan. Are you aware about it?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_home_loan"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 57
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            57 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "58. Did you get any assistance for the Purchase/ Built of House under Housing Scheme?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "assistance_home_loan"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 58
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            58 -> {
//                personalQuectionBinding.questions.text =
//                    "59. If Yes, have you built/ purchased the house under Housing scheme?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Under construction"
//                        )
//                    ), "built_house_under_home_loan"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 59
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            59 -> {
//                personalQuectionBinding.questions.text =
//                    "60. Was the financial assistance helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient",
//                            "Partially"
//                        )
//                    ), "meet_the_need_home_loan"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 60
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            60 -> {
//                personalQuectionBinding.questions.text =
//                    "61. Does this scheme being loan assistance deter you from availing it?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "availing_home_loan"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 61
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            61 -> {
//                position = 62
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "62. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            62 -> {
//                hideKeyboard()
//                editValueField["home_loan_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 63
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "63. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            63 -> {
//                hideKeyboard()
//                editValueField["home_loan_challenge_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text = "Disability Pension Scheme"
//                personalQuectionBinding.questions.text =
//                    "64. Are you aware about Disability Pension Scheme of UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_disability_pension_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 64
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            64 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "65. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "assistance_disability_pension_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 65
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            65 -> {
//                personalQuectionBinding.questions.text =
//                    "66. Was the financial assistance helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient",
//                            "Partially"
//                        )
//                    ), "meet_disability_pension_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 66
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            66 -> {
//                position = 67
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "67.What are the impacts of this scheme on your condition?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            67 -> {
//                hideKeyboard()
//                editValueField["disability_pension_scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 68
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "68. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            68 -> {
//                hideKeyboard()
//                editValueField["disability_pension_scheme_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Financial Assistance due to Death"
//                personalQuectionBinding.questions.text =
//                    "69. Are you aware about Financial Assistance due to Death (Rs. 20,000 for Natural Death & Rs.50,000 for Accidental death of registered construction worker) by UKBOCWWB?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_death_financial_help"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 69
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            69 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "70. Have you availed the benefits under this scheme?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "availed_death_financial_help"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 70
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            70 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "71. How many times you availed the benefits?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "One Time",
//                            "Two Times",
//                            "Three times",
//                            "More than three times"
//                        )
//                    ), "times_availed_death_financial_help"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 71
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            71 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "72. Was the financial assistance helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient",
//                            "Partially",
//                        )
//                    ), "meet_need_death_financial_help"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 72
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            72 -> {
//                position = 73
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "73. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            73 -> {
//                hideKeyboard()
//                editValueField["death_financial_help_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 74
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "74. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            74 -> {
//                hideKeyboard()
//                editValueField["death_financial_help_challenge_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Funeral Assistance"
//                personalQuectionBinding.questions.text =
//                    "75. The UKBOCWWB provides Funeral Assistance (Rs 10,000). Are you aware about the Assistance Scheme?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_funeral_assistance"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 75
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            75 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "76. Does the lengthy process and the benefit be given only once a year deter you from availing the benefit?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                        )
//                    ), "availed_funeral_assistance"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 76
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            76 -> {
//                position = 77
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "77. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            77 -> {
//                hideKeyboard()
//                editValueField["death_financial_help_impacts"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Tool Kit Assistance"
//                personalQuectionBinding.questions.text =
//                    "78. Are you aware about Tool Kit Assistance by UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_tool_kit"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 78
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            78 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "79. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                        )
//                    ), "availed_tool_kit"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 79
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            79 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "80. Was the financial assistance helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient",
//                            "Partially"
//                        )
//                    ), "meet_need_tool_kit"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 80
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            80 -> {
//                position = 81
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "81. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            81 -> {
//                hideKeyboard()
//                editValueField["tool_kit_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 83
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "82. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            83 -> {
//                hideKeyboard()
//                editValueField["tool_kit_challenge_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Bicycle Scheme"
//                personalQuectionBinding.questions.text =
//                    "83. Are you aware about Bicycle Scheme (Construction workers of Plain area registered workers) by UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_bicycle_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 84
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            84 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "84. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "availed_bicycle_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 85
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            85 -> {
//                position = 86
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "85. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            86 -> {
//                hideKeyboard()
//                editValueField["bicycle_scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 87
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "86. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            87 -> {
//                hideKeyboard()
//                editValueField["tool_kit_challenge_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Sewing Machine Scheme"
//                personalQuectionBinding.questions.text =
//                    "87. Are you aware about Sewing Machine Scheme (Sewing machine to construction workers of mountainous areas or their dependents) by UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_sewing_machine_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 88
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            88 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "88. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "avail_sewing_machine_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 89
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            89 -> {
//                position = 90
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "89. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            90 -> {
//                hideKeyboard()
//                editValueField["sewing_machine_scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 91
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "90. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            91 -> {
//                hideKeyboard()
//                editValueField["aware_sewing_machine_scheme_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Sanitary Napkin Scheme"
//                personalQuectionBinding.questions.text =
//                    "91. Are you aware about Sanitary Napkin Scheme (Sanitary napkins to the registered female workers or the daughters of registered workers) by UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_sanitary_napkins_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 92
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            92 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "92. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "avail_sanitary_napkins_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 93
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            93 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "93. Was the assistance helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient",
//                            "Partially"
//                        )
//                    ), "needs_sanitary_napkins_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 94
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            94 -> {
//                position = 95
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "94. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            95 -> {
//                hideKeyboard()
//                editValueField["sanitary_napkins_scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 96
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "95. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            96 -> {
//                hideKeyboard()
//                editValueField["sanitary_napkins_scheme_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Skill Training Scheme"
//                personalQuectionBinding.questions.text =
//                    "96. Are you aware about Skill Training (collaboration with NIESBUD to train & form SHG groups of female members of registered workers under UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_skill_training_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 97
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            97 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "97. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "avail_skill_training_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 98
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            98 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "98. Was the Skill Training helpful?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Sufficient",
//                            "Not Sufficient ",
//                            "Partially"
//                        )
//                    ), "helpful_skill_training_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 99
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            99 -> {
//                position = 100
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "99. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            100 -> {
//                hideKeyboard()
//                editValueField["skill_training_scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 101
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "100. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            101 -> {
//                hideKeyboard()
//                editValueField["skill_training_scheme_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Solar Light Scheme"
//                personalQuectionBinding.questions.text =
//                    "101. Are you aware about Solar Light Scheme (once in lifetime) by UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_solar_light_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 102
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            102 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "102. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "avail_solar_light_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 103
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            103 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "103. Was the Scheme helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Partially",
//                            "Not Sufficient ",
//                            "Sufficient"
//                        )
//                    ), "helpful_solar_light_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 104
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            104 -> {
//                position = 105
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "104. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            105 -> {
//                hideKeyboard()
//                editValueField["solar_light_scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 106
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "105. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            106 -> {
//                hideKeyboard()
//                editValueField["solar_light_scheme_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Umbrella Scheme"
//                personalQuectionBinding.questions.text =
//                    "106. Are you aware about Umbrella Scheme (once in a lifetime) by UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_Umbrella_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 107
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            107 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "107. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "avail_Umbrella_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 108
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            108 -> {
//                position = 109
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "108. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            109 -> {
//                hideKeyboard()
//                editValueField["Umbrella_scheme_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 110
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "109. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            110 -> {
//                hideKeyboard()
//                editValueField["solar_light_scheme_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Skill Up gradation Scheme"
//                personalQuectionBinding.questions.text =
//                    "110. Are you aware about Skill Up gradation by UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_skill_up_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 111
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            111 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "111. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "avail_skill_up_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 112
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            112 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "112. Was the Scheme helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Partially",
//                            "Not Sufficient ",
//                            "Sufficient"
//                        )
//                    ), "helpful_skill_up_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 113
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            113 -> {
//                position = 114
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "113. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            114 -> {
//                hideKeyboard()
//                editValueField["skill_up_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 115
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "114. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            115 -> {
//                hideKeyboard()
//                editValueField["skill_up_scheme_faced"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                personalQuectionBinding.etAnswer.isVisible = false
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.toolbarLayout.tvToolbar.text =
//                    "Construction of Toilets"
//                personalQuectionBinding.questions.text =
//                    "115. Are you aware about Construction of Toilets (Rs. 12,000 for construction of toilets to registered eligible construction workers) by UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No"
//                        )
//                    ), "aware_washroom_construction_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 116
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            116 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "116. Have you got benefits of the scheme from UKBOCW Welfare Board?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Yes",
//                            "No",
//                            "Not Eligible"
//                        )
//                    ), "avail_washroom_construction_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 117
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            117 -> {
//                personalQuectionBinding.rvOtherAnswer.isVisible = false
//                personalQuectionBinding.tvOtherAnswer.isVisible = false
//                personalQuectionBinding.questions.text =
//                    "117. Was the Scheme helpful to meet the needs?"
//                personalQuectionBinding.rvQuestions.isVisible = true
//                questionAdapter.addOtherQuestion(
//                    QuestionOptionType(
//                        arrayListOf(
//                            "Partially",
//                            "Not Sufficient ",
//                            "Sufficient"
//                        )
//                    ), "helpful_washroom_construction_scheme"
//                )
//                questionAdapter.notifyDataSetChanged()
//                position = 118
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            118 -> {
//                position = 119
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.rvQuestions.isVisible = false
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "118. What are the impacts of this scheme?"
//                personalQuectionBinding.etAnswer.hint = "Impacts of this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            119 -> {
//                hideKeyboard()
//                editValueField["washroom_construction_impact"] =
//                    personalQuectionBinding.etAnswer.text.toString()
//                position = 120
//                personalQuectionBinding.etAnswer.text.clear()
//                personalQuectionBinding.etAnswer.isVisible = true
//                personalQuectionBinding.questions.text =
//                    "119. What are the challenges you faced to avail the scheme and your opinion, views, and suggestions about the scheme?"
//                personalQuectionBinding.etAnswer.hint = "Challenge faced to availed this scheme"
//                personalQuectionBinding.button.text = "Next"
//            }
//
//            else -> {
//                position = 0
//            }
//        }
//    }

}

interface QuestionClickListener {
    fun getOptionItemClicked(option: String, question: String)
}