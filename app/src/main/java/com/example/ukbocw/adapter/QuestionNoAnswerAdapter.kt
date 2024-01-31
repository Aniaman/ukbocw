package com.example.ukbocw.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.ukbocw.QuestionClickListener
import com.example.ukbocw.R
import com.example.ukbocw.databinding.OptionsTypeLayoutBinding
import com.example.ukbocw.model.QuestionOptionType

class QuestionNoAnswerAdapter(
    private var questionType: QuestionOptionType,
    private val listener: QuestionClickListener,
    private var questionName: String
) : RecyclerView.Adapter<QuestionNoAnswerAdapter.AnswerNoHolder>() {
    private var selectedPosition = RecyclerView.NO_POSITION
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuestionNoAnswerAdapter.AnswerNoHolder {
        val binding =
            OptionsTypeLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnswerNoHolder(binding)
    }

    override fun getItemCount(): Int {
        return questionType.options.size
    }

    override fun onBindViewHolder(holder: QuestionNoAnswerAdapter.AnswerNoHolder, position: Int) {
        if (questionType.options.isNotEmpty()) {
            holder.questionTypeLayoutBinding.questionOptions.isVisible = true
            holder.questionTypeLayoutBinding.questionOptions.text =
                questionType.options[position]

            // Set the background based on the selected position
            if (position == selectedPosition) {
                holder.questionTypeLayoutBinding.questionOptions.setBackgroundResource(R.drawable.btn_enable_disable)
            } else {
                holder.questionTypeLayoutBinding.questionOptions.setBackgroundResource(R.drawable.white_bg_gray_border)
            }

            holder.questionTypeLayoutBinding.questionOptions.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)

                // Check if the clicked position is the same as the selected position
                if (position == selectedPosition) {
                    holder.questionTypeLayoutBinding.questionOptions.setBackgroundResource(R.drawable.btn_enable_disable)
                    listener.getOptionItemClicked(questionType.options[position], questionName)
                } else {
                    holder.questionTypeLayoutBinding.questionOptions.setBackgroundResource(R.drawable.white_bg_gray_border)
                }
            }
        }
    }


    inner class AnswerNoHolder(val questionTypeLayoutBinding: OptionsTypeLayoutBinding) :
        RecyclerView.ViewHolder(questionTypeLayoutBinding.root)
}