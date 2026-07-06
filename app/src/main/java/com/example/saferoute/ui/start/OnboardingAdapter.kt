package com.example.saferoute.ui.start

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R

class OnboardingAdapter(
    private val onSosClick: () -> Unit,
    private val onLocationClick: () -> Unit,
    private val onFinalGetStartedClick: () -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    private val layouts = arrayOf(
        R.layout.item_onboarding_sos,
        R.layout.item_onboarding_location,
        R.layout.item_onboarding_features
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        when (position) {
            0 -> {

                val btnGetStarted = holder.itemView.findViewById<Button>(R.id.btnGetStarted)
                btnGetStarted?.setOnClickListener { onSosClick() }
            }
            1 -> {

                val btnContinue = holder.itemView.findViewById<Button>(R.id.btnContinue)
                btnContinue?.setOnClickListener { onLocationClick() }
            }
            2 -> {

                val btnFinalGetStarted = holder.itemView.findViewById<Button>(R.id.btnFinalGetStarted)
                btnFinalGetStarted?.setOnClickListener { onFinalGetStartedClick() }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = layouts[position]

    override fun getItemCount(): Int = layouts.size

    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view)
}