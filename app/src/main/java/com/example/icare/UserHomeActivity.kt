package com.example.icare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.icare.adapter.AdapterDoctor
import com.example.icare.adapter.AdapterNews
import com.example.icare.databinding.ActivityUserHomeBinding
import com.example.icare.model.ModelDoctor
import com.example.icare.model.ModelNews
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class UserHomeActivity : AppCompatActivity() {
    private var binding: ActivityUserHomeBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    private lateinit var viewPager: ViewPager2
    private lateinit var newsAdapter: AdapterNews

    private lateinit var viewPager2: ViewPager2
    private lateinit var doctorAdapter: AdapterDoctor

    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_home)

        binding = ActivityUserHomeBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val email = sharedPref.getString("email", "")

        val headerView = binding!!.navigationView.getHeaderView(0)
        val nameTv : TextView = headerView.findViewById(R.id.nameTv)
        val emailTv : TextView = headerView.findViewById(R.id.emailTv)

        nameTv.text = name
        emailTv.text = email

        binding!!.titleDashboardUser.text = name
        binding!!.subtitleDashboardUser.text = email

        val drawer = binding!!.myDrawerLayout
        val imageButton = binding!!.menuBtn
        imageButton.setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        val navigationView = binding!!.navigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            if (id == R.id.nav_home) {
                drawer.closeDrawer(GravityCompat.START)
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@UserHomeActivity, AppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                val intent = Intent(this@UserHomeActivity, ProfileActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.nav_aboutus) {
                startActivity(Intent(this@UserHomeActivity, AboutUsActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@UserHomeActivity, HomeActivity::class.java)
                firebaseAuth!!.signOut()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            true
        }

        viewPager = binding!!.viewPager
        newsAdapter = AdapterNews(this@UserHomeActivity, getSampleNews())
        viewPager.adapter = newsAdapter
        binding!!.dotsIndicator.setViewPager2(viewPager!!)

        viewPager2 = binding!!.viewPager2
        doctorAdapter = AdapterDoctor(this@UserHomeActivity, getSampleDoctor())
        viewPager2.adapter = doctorAdapter
        binding!!.dotsIndicator2.setViewPager2(viewPager2!!)
    }

    private fun getSampleDoctor(): List<ModelDoctor> {
        return listOf(
            ModelDoctor(R.drawable.img_doctor1, "Dr. Irvine Wong", "31 years of experience in cardiology", R.drawable.img_doctor2, "Dr. Lee Chun Kiat", "24 years of experience in dermatology"),
            ModelDoctor(R.drawable.img_doctor3, "Dr. Beatrice Lai", "21 years of experience in gynecology", R.drawable.img_doctor4, "Dr. Pierre Lim", "18 years of experience in neurology"),
            ModelDoctor(R.drawable.img_doctor5, "Dr. Wilfred Yong", "15 years of experience om urology", R.drawable.img_doctor6, "Dr. Vivian Chin", "11 years of experience in orthopedics")
        )
    }

    private fun getSampleNews(): List<ModelNews> {
        return listOf(
            ModelNews(R.drawable.img_news1, "At our clinic, excellence knows no borders. With a team of healthcare professionals hailing from diverse corners of the globe, we bring a wealth of international expertise to provide comprehensive and compassionate care for our patients."),
            ModelNews(R.drawable.img_news2, "At the forefront of healthcare innovation, our clinic integrates cutting-edge technology to redefine patient care. Equipped with the latest advancements in the industry, we ensure precision, efficiency, and state-of-the-art solutions for your well-being."),
            ModelNews(R.drawable.img_news3, "Our clinic is dedicated to catering to all age groups with specialized care. Our skilled healthcare professionals possess the expertise to address diverse healthcare needs, ensuring tailored treatments for patients of every age. Your well-being is our priority across the generations.")
        )
    }

    private fun clearSharedPreferences() {
        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove("email")
        editor.remove("name")
        editor.remove("role")
        editor.apply()
    }
}