package com.example.fypapplication.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fypapplication.student.PastMeetingsFragment
import com.example.fypapplication.student.UpcomingMeetingsFragment

class MeetingsPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UpcomingMeetingsFragment()
            1 -> PastMeetingsFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}