package com.example.ukbocw

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.example.ukbocw.databinding.ActivityPersonalQuectionBinding
import com.example.ukbocw.model.FamilyMemberList

class PersonalQuection : AppCompatActivity() {
    lateinit var personalQuectionBinding: ActivityPersonalQuectionBinding
    private var selectedGender: String? = null
    private var selectedAge: String? = null
    private var selectedArea: String? = null
    private var selectedEducation: String? = null
    private var selectedDesignation: String? = null
    private var selectedReligion: String? = null
    private var selectedRationCard: String? = null
    private var selectedSourceOfIncome: String? = null
    private var selectedland: String? = null
    private var selectedhome: String? = null
    private var selectedhouseType: String? = null
    private var selectedBasicAmenities: String? = null
    private var selectedWashroom: String? = null
    private var selectedFamilyIncome: String? = null
    private var selectedSocialCategory: String? = null
    private var selectedmedical: String? = null
    private var selectedMemberOccupation: String? = null
    private var selectedMemberEducation: String? = null
    private var selectedMaritalStatus: String? = null
    private var selectedfaceProblemCertificate: String? = null
    private var selectedmembership: String? = null
    private var selectednoOfYears: String? = null
    private var selectedCertificateReduce: String? = null
    private var selectedFillForm: String? = null
    private var selectedRenewMembership: String? = null
    private var selectedRenewMembershipReason: String? = null
    private var selectedSchemeAware: String? = null
    private var selectedSchemeAwareBoard: String? = null
    private var selectedSchemeAwareWhere: String? = null
    private var selectedMaternitySchemeAware: String? = null
    private var selectedMaternitySchemeAvail: String? = null
    private var selectedMaternitySchemeAvailReason: String? = null
    private var selectedetOtherFundUtilized: String? = null
    private var selectedFinancialAssistance: String? = null
    var position = 0
    private val memberList = mutableListOf<FamilyMemberList>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        personalQuectionBinding = ActivityPersonalQuectionBinding.inflate(layoutInflater)
        setContentView(personalQuectionBinding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = getColor(R.color.white)

        //Dropdown list


        val genderAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.gender))
        personalQuectionBinding.basicDetail.dropdownGender.setAdapter(genderAdapter)
        personalQuectionBinding.basicDetail.dropdownGender.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedGender = adapterView.getItemAtPosition(i).toString()
            }


        val ageAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.age))
        personalQuectionBinding.basicDetail.dropdownAge.setAdapter(ageAdapter)
        personalQuectionBinding.basicDetail.dropdownAge.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedAge = adapterView.getItemAtPosition(i).toString()
            }


        val areaAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.area))
        personalQuectionBinding.basicDetail.dropdownArea.setAdapter(areaAdapter)
        personalQuectionBinding.basicDetail.dropdownArea.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedArea = adapterView.getItemAtPosition(i).toString()
            }


        val maritalStatusAdapter = ArrayAdapter(
            this,
            R.layout.list_layout,
            resources.getStringArray(R.array.maritalStatus)
        )
        personalQuectionBinding.basicDetail.dropdownMaritalStatus.setAdapter(maritalStatusAdapter)
        personalQuectionBinding.basicDetail.dropdownMaritalStatus.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedMaritalStatus = adapterView.getItemAtPosition(i).toString()
            }


        val educationAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.education))
        personalQuectionBinding.basicDetail.dropdownEducation.setAdapter(educationAdapter)
        personalQuectionBinding.basicDetail.dropdownEducation.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedEducation = adapterView.getItemAtPosition(i).toString()
            }

        val designationAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.designation))
        personalQuectionBinding.basicDetail.dropdownDesignation.setAdapter(designationAdapter)
        personalQuectionBinding.basicDetail.dropdownDesignation.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedDesignation = adapterView.getItemAtPosition(i).toString()
                if (selectedDesignation == "Other") {
                    personalQuectionBinding.basicDetail.etOtherDesignation.visibility = View.VISIBLE
                } else {
                    personalQuectionBinding.basicDetail.etOtherDesignation.visibility = View.GONE
                }
            }


        val socialCategoryAdapter = ArrayAdapter(
            this,
            R.layout.list_layout,
            resources.getStringArray(R.array.socialCategory)
        )
        personalQuectionBinding.basicDetail.dropdownSocialCategory.setAdapter(socialCategoryAdapter)
        personalQuectionBinding.basicDetail.dropdownSocialCategory.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedSocialCategory = adapterView.getItemAtPosition(i).toString()
            }


        val religionAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.religion))
        personalQuectionBinding.basicDetail.dropdownReligion.setAdapter(religionAdapter)
        personalQuectionBinding.basicDetail.dropdownReligion.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedReligion = adapterView.getItemAtPosition(i).toString()
                if (selectedReligion == "Other") {
                    personalQuectionBinding.basicDetail.etOtherReligion.visibility = View.VISIBLE
                } else {
                    personalQuectionBinding.basicDetail.etOtherReligion.visibility = View.GONE
                }
            }


        val stateAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.state))
        personalQuectionBinding.basicDetail.dropdownState.setAdapter(stateAdapter)
        personalQuectionBinding.basicDetail.dropdownState.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedReligion = adapterView.getItemAtPosition(i).toString()
                personalQuectionBinding.button.visibility = View.VISIBLE
                if (selectedReligion == "Other") {
                    personalQuectionBinding.basicDetail.etOtherState.visibility = View.VISIBLE
                } else {
                    personalQuectionBinding.basicDetail.etOtherState.visibility = View.GONE
                }
            }

        personalQuectionBinding.familyMember.tvAddMember.setOnClickListener {
            setFamilyMember()
        }
        personalQuectionBinding.button.setOnClickListener {
            setFormWizzard()
        }
        personalQuectionBinding.familyMember.tvSave.setOnClickListener {
            personalQuectionBinding.button.visibility = View.VISIBLE
            personalQuectionBinding.button.text = "Next"
        }
    }

    private fun setFormWizzard() {
        when (position) {
            0 -> {
                personalQuectionBinding.button.visibility = View.GONE
                setFamilyMemberDropdown()
                personalQuectionBinding.basicDetailLayout.visibility = View.GONE
                personalQuectionBinding.familyMemberLayout.visibility = View.VISIBLE
                position = 1
                personalQuectionBinding.button.text = "Next"
            }

            1 -> {
                personalQuectionBinding.button.visibility = View.GONE
                setEconomicStatus()
                personalQuectionBinding.familyMemberLayout.visibility = View.GONE
                personalQuectionBinding.economicStatusLayout.visibility = View.VISIBLE
                position = 2
                personalQuectionBinding.button.text = "Next"
            }

            2 -> {
                personalQuectionBinding.button.visibility = View.GONE
                setRegistrationDropdown()
                personalQuectionBinding.economicStatusLayout.visibility = View.GONE
                personalQuectionBinding.registrationQuestionLayout.visibility = View.VISIBLE
                position = 3
                personalQuectionBinding.button.text = "Next"
            }

            3 -> {
                personalQuectionBinding.button.visibility = View.GONE
                setMaternityDropdown()
                personalQuectionBinding.registrationQuestionLayout.visibility = View.GONE
                personalQuectionBinding.maternitySchemeLayout.visibility = View.VISIBLE
                position = 4
                personalQuectionBinding.button.text = "Next"
            }

            else -> {
                position = 0
            }
        }
    }

    private fun setMaternityDropdown() {
        val maternitySchemeAwareAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNo))
        personalQuectionBinding.maternityScheme.dropdownMaternitySchemeAware.setAdapter(
            maternitySchemeAwareAdapter
        )
        personalQuectionBinding.maternityScheme.dropdownMaternitySchemeAware.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedMaternitySchemeAware = adapterView.getItemAtPosition(i).toString()
                if (selectedMaternitySchemeAware == "No") {
                    personalQuectionBinding.maternityScheme.tvAvailWhy.visibility = View.VISIBLE
                    personalQuectionBinding.maternityScheme.tilAvailReasonWhy.visibility =
                        View.VISIBLE
                } else {
                    personalQuectionBinding.maternityScheme.tvAvailWhy.visibility = View.GONE
                    personalQuectionBinding.maternityScheme.tilAvailReasonWhy.visibility = View.GONE
                }
            }
        val maternitySchemeAvailAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNoAvail))
        personalQuectionBinding.maternityScheme.dropdownMaternitySchemeAvail.setAdapter(
            maternitySchemeAvailAdapter
        )
        personalQuectionBinding.maternityScheme.dropdownMaternitySchemeAvail.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedMaternitySchemeAvail = adapterView.getItemAtPosition(i).toString()
                if (selectedMaternitySchemeAvail == "No") {
                    personalQuectionBinding.maternityScheme.tvAvailWhy.visibility = View.VISIBLE
                    personalQuectionBinding.maternityScheme.tilAvailReasonWhy.visibility =
                        View.VISIBLE
                } else {
                    personalQuectionBinding.maternityScheme.tvAvailWhy.visibility = View.GONE
                    personalQuectionBinding.maternityScheme.tilAvailReasonWhy.visibility = View.GONE
                }
            }

        val whyNotAvailAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.whyNotAvail))
        personalQuectionBinding.maternityScheme.dropdownMaternitySchemeAvailReason.setAdapter(
            whyNotAvailAdapter
        )
        personalQuectionBinding.maternityScheme.dropdownMaternitySchemeAvailReason.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedMaternitySchemeAvailReason = adapterView.getItemAtPosition(i).toString()
                if (selectedMaternitySchemeAvailReason == "Others") {
                    personalQuectionBinding.maternityScheme.otherReason.visibility = View.VISIBLE
                    personalQuectionBinding.maternityScheme.etOtherReason.visibility =
                        View.VISIBLE
                } else {
                    personalQuectionBinding.maternityScheme.otherReason.visibility = View.GONE
                    personalQuectionBinding.maternityScheme.etOtherReason.visibility = View.GONE
                }
            }
        val fundUtilizedAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.fundUtilized))
        personalQuectionBinding.maternityScheme.fundUtilized.setAdapter(
            fundUtilizedAdapter
        )
        personalQuectionBinding.maternityScheme.fundUtilized.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedetOtherFundUtilized = adapterView.getItemAtPosition(i).toString()
                if (selectedetOtherFundUtilized == "Others") {
                    personalQuectionBinding.maternityScheme.otherFundUtilized.visibility =
                        View.VISIBLE
                    personalQuectionBinding.maternityScheme.etOtherFundUtilized.visibility =
                        View.VISIBLE
                } else {
                    personalQuectionBinding.maternityScheme.otherFundUtilized.visibility = View.GONE
                    personalQuectionBinding.maternityScheme.etOtherFundUtilized.visibility =
                        View.GONE
                }
            }
        val financialAssistanceAdapter =
            ArrayAdapter(
                this,
                R.layout.list_layout,
                resources.getStringArray(R.array.financialAssistance)
            )
        personalQuectionBinding.maternityScheme.dropdownFinancialAssistance.setAdapter(
            financialAssistanceAdapter
        )
        personalQuectionBinding.maternityScheme.dropdownFinancialAssistance.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedFinancialAssistance = adapterView.getItemAtPosition(i).toString()
            }

    }

    private fun setRegistrationDropdown() {

        val noOfYearsAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.years))
        personalQuectionBinding.registrationQuestion.dropdownNumberOfYears.setAdapter(
            noOfYearsAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownNumberOfYears.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectednoOfYears = adapterView.getItemAtPosition(i).toString()
            }


        val membershipAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNo))
        personalQuectionBinding.registrationQuestion.dropdownRegisteringProblem.setAdapter(
            membershipAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownRegisteringProblem.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedmembership = adapterView.getItemAtPosition(i).toString()
            }


        val faceProblemCertificateAdapter =
            ArrayAdapter(
                this,
                R.layout.list_layout,
                resources.getStringArray(R.array.faceProblemCertificate)
            )
        personalQuectionBinding.registrationQuestion.dropdownCertificateProblem.setAdapter(
            faceProblemCertificateAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownCertificateProblem.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedfaceProblemCertificate = adapterView.getItemAtPosition(i).toString()
            }


        val certificateReduceAdapter =
            ArrayAdapter(
                this,
                R.layout.list_layout,
                resources.getStringArray(R.array.faceProblemWorking)
            )
        personalQuectionBinding.registrationQuestion.dropdownCertificateReduceDay.setAdapter(
            certificateReduceAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownCertificateReduceDay.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedCertificateReduce = adapterView.getItemAtPosition(i).toString()
            }


        val fillFormsAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.fillForm))
        personalQuectionBinding.registrationQuestion.dropdownFillTheForm.setAdapter(
            fillFormsAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownFillTheForm.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedFillForm = adapterView.getItemAtPosition(i).toString()
            }


        val renewMembershipAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNo))
        personalQuectionBinding.registrationQuestion.dropdownRenewMembership.setAdapter(
            renewMembershipAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownRenewMembership.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedRenewMembership = adapterView.getItemAtPosition(i).toString()
            }


        val renewMembershipReasonAdapter =
            ArrayAdapter(
                this,
                R.layout.list_layout,
                resources.getStringArray(R.array.renewMembershipNo)
            )
        personalQuectionBinding.registrationQuestion.dropdownRenewMembershipNoReason.setAdapter(
            renewMembershipReasonAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownRenewMembershipNoReason.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedRenewMembershipReason = adapterView.getItemAtPosition(i).toString()
            }


        val schemeAwareAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNoSchemes))
        personalQuectionBinding.registrationQuestion.dropdownSchemeAware.setAdapter(
            schemeAwareAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownSchemeAware.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedSchemeAware = adapterView.getItemAtPosition(i).toString()
            }


        val schemeAwareWhereAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.schemesKnow))
        personalQuectionBinding.registrationQuestion.dropdownSchemeAwareWhere.setAdapter(
            schemeAwareWhereAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownSchemeAwareWhere.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedSchemeAwareWhere = adapterView.getItemAtPosition(i).toString()
            }


        val schemeAwareBoardAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNo))
        personalQuectionBinding.registrationQuestion.dropdownSchemeAwareFromBoard.setAdapter(
            schemeAwareBoardAdapter
        )
        personalQuectionBinding.registrationQuestion.dropdownSchemeAwareFromBoard.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedSchemeAwareBoard = adapterView.getItemAtPosition(i).toString()
                personalQuectionBinding.button.isVisible = true
            }
    }

    private fun setEconomicStatus() {

        val rationCardAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.rationCard))
        personalQuectionBinding.economicStatus.dropdownRationCard.setAdapter(rationCardAdapter)
        personalQuectionBinding.economicStatus.dropdownRationCard.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedRationCard = adapterView.getItemAtPosition(i).toString()
            }

        val sourceOfIncomeAdapter = ArrayAdapter(
            this,
            R.layout.list_layout,
            resources.getStringArray(R.array.sourceOfIncome)
        )
        personalQuectionBinding.economicStatus.dropdownSourceOfIncome.setAdapter(
            sourceOfIncomeAdapter
        )
        personalQuectionBinding.economicStatus.dropdownSourceOfIncome.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedSourceOfIncome = adapterView.getItemAtPosition(i).toString()
            }

        val familyIncomeAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.familyIncome))
        personalQuectionBinding.economicStatus.dropdownFamilyIncome.setAdapter(familyIncomeAdapter)
        personalQuectionBinding.economicStatus.dropdownFamilyIncome.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedFamilyIncome = adapterView.getItemAtPosition(i).toString()
            }

        val medicalAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.medical))
        personalQuectionBinding.economicStatus.dropdownMedical.setAdapter(medicalAdapter)
        personalQuectionBinding.economicStatus.dropdownMedical.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedmedical = adapterView.getItemAtPosition(i).toString()
            }

        val landAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNo))
        personalQuectionBinding.economicStatus.dropdownLandPosses.setAdapter(landAdapter)
        personalQuectionBinding.economicStatus.dropdownLandPosses.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedland = adapterView.getItemAtPosition(i).toString()
            }

        val homeAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.home))
        personalQuectionBinding.economicStatus.dropdownLandPosses.setAdapter(homeAdapter)
        personalQuectionBinding.economicStatus.dropdownLandPosses.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedhome = adapterView.getItemAtPosition(i).toString()
            }

        val houseTypeAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.houseType))
        personalQuectionBinding.economicStatus.dropdownHouseType.setAdapter(houseTypeAdapter)
        personalQuectionBinding.economicStatus.dropdownHouseType.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedhouseType = adapterView.getItemAtPosition(i).toString()
            }

        val washroomAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNo))
        personalQuectionBinding.economicStatus.dropdownAnyWashroom.setAdapter(washroomAdapter)
        personalQuectionBinding.economicStatus.dropdownAnyWashroom.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedWashroom = adapterView.getItemAtPosition(i).toString()
            }

        val basicAmenitiesAdapter =
            ArrayAdapter(this, R.layout.list_layout, resources.getStringArray(R.array.YesNo))
        personalQuectionBinding.economicStatus.dropdownAmenities.setAdapter(basicAmenitiesAdapter)
        personalQuectionBinding.economicStatus.dropdownAmenities.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedBasicAmenities = adapterView.getItemAtPosition(i).toString()
                personalQuectionBinding.button.isVisible = true
            }
    }

    private fun setFamilyMemberDropdown() {
        val memberOccupationAdapter = ArrayAdapter(
            this,
            R.layout.list_layout,
            resources.getStringArray(R.array.memberOccupation)
        )
        personalQuectionBinding.familyMember.dropdownOccupation.setAdapter(memberOccupationAdapter)
        personalQuectionBinding.familyMember.dropdownOccupation.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedMemberOccupation = adapterView.getItemAtPosition(i).toString()
                if (selectedMemberOccupation == "Other") {
                    personalQuectionBinding.familyMember.etOtherOccupation.visibility = View.VISIBLE
                } else {
                    personalQuectionBinding.familyMember.etOtherOccupation.visibility = View.GONE
                }
            }

        val memberEducationAdapter = ArrayAdapter(
            this,
            R.layout.list_layout,
            resources.getStringArray(R.array.memberEducation)
        )
        personalQuectionBinding.familyMember.dropdownEducation.setAdapter(memberEducationAdapter)
        personalQuectionBinding.familyMember.dropdownEducation.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                selectedMemberEducation = adapterView.getItemAtPosition(i).toString()
            }
    }

    private fun setFamilyMember() {
        memberList.add(
            FamilyMemberList(
                personalQuectionBinding.familyMember.etFullName.text.toString(),
                selectedMemberOccupation,
                personalQuectionBinding.familyMember.etOtherOccupation.text.toString(),
                selectedMemberOccupation
            )
        )
    }
}