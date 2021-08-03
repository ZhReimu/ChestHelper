package com.mrx.clashRoyal

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private val mHandler = Handler(Looper.getMainLooper())
    private var chestHelper = ClashRoyaleChestHelper("")

    private val saveData = { value: String ->
        getSharedPreferences("userTagSave", MODE_PRIVATE).edit().putString("userTag", value).apply()
    }

    private val getData = {
        getSharedPreferences("userTagSave", MODE_PRIVATE).getString("userTag", null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val swp: SwipeRefreshLayout = findViewById(R.id.swp)
        val chestList: RecyclerView = findViewById(R.id.chestList)
        val btQuery: Button = findViewById(R.id.start)
        val edTAG: TextView = findViewById(R.id.edUserTag)

        if (getData() != null) {
            edTAG.text = getData()
        }
        // 设置下拉刷新监听器，下拉刷新时更新用户信息以及宝箱信息
        swp.setOnRefreshListener {
            val userTag = edTAG.text.toString().trim()
            if (!TextUtils.isEmpty(userTag)) {
                if (chestHelper.userTAG != userTag) {
                    chestHelper = ClashRoyaleChestHelper(userTag)
                }
                Toast.makeText(this, "正在更新用户资料", Toast.LENGTH_LONG).show()
                chestHelper.refreshData { status, refreshResult ->
                    mHandler.post {
                        swp.isRefreshing = false
                        if (status == ClashRoyaleChestHelper.STATUS_SUCCESS) {
                            when {
                                refreshResult!!.status -> {
                                    Toast.makeText(
                                        this,
                                        "更新用户资料成功！\n${refreshResult.msg} \n上次更新时间为 ${refreshResult.lastUpdateDate}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    btQuery.callOnClick()
                                }
                                status == ClashRoyaleChestHelper.STATUS_FAILURE_USER_NOT_EXISTS -> {
                                    Toast.makeText(
                                        this,
                                        "更新用户资料失败，\n用户不存在！",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                else -> {
                                    Toast.makeText(
                                        this,
                                        "更新用户资料失败，更新间隔过短！\n${refreshResult.msg} \n上次更新时间为 ${refreshResult.lastUpdateDate}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "更新用户资料失败, 网络错误！", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "请输入用户标签！", Toast.LENGTH_LONG).show()
            }
        }
        // 设置布局管理器为网格布局，实现一行显示两个宝箱信息
        chestList.layoutManager =
            GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)
        // 设置案件监听事件，点击 查宝箱 按钮时开始查宝箱
        btQuery.setOnClickListener {
            val userTag = edTAG.text.toString().trim()
            if (!TextUtils.isEmpty(userTag)) {
                saveData(userTag)
                // 重置 actionBar 的内容
                supportActionBar?.title = getString(R.string.app_name)
                // 开始查宝箱之前显示下拉刷新球
                swp.isRefreshing = true
                // 点击按钮后禁用按钮，防止多次点击
                btQuery.isEnabled = false
                Toast.makeText(this, "正在获取宝箱信息", Toast.LENGTH_LONG).show()
                if (chestHelper.userTAG != userTag) {
                    chestHelper = ClashRoyaleChestHelper(userTag)
                }
                chestHelper.getChestData { status, userName, chestData ->
                    when (status) {
                        ClashRoyaleChestHelper.STATUS_SUCCESS -> {
                            mHandler.post {
                                Toast.makeText(this, "宝箱信息获取成功！", Toast.LENGTH_SHORT).show()
                                chestList.adapter = ChestAdapter(this, chestData!!)
                                // 设置 supportActionBar
                                supportActionBar?.title = "$userName 的宝箱信息"
                            }
                        }
                        ClashRoyaleChestHelper.STATUS_FAILURE_USER_NOT_EXISTS -> {
                            mHandler.post {
                                Toast.makeText(this, "宝箱信息获取失败，用户不存在！", Toast.LENGTH_SHORT).show()
                                supportActionBar?.title = userName
                            }
                        }
                        else -> {
                            mHandler.post {
                                Toast.makeText(this, "宝箱信息获取失败，网络错误！", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    // 任务结束后 取消下拉刷新球，启用按钮
                    mHandler.post {
                        swp.isRefreshing = false
                        btQuery.isEnabled = true
                    }
                }
            } else {
                Toast.makeText(this, "请输入用户标签！", Toast.LENGTH_LONG).show()
            }
        }
    }

}