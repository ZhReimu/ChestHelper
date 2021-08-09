package com.mrx.clashRoyal

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mrx.clashRoyal.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val mHandler = Handler(Looper.getMainLooper())
    private var chestHelper: ClashRoyaleChestHelper? = null
    private lateinit var edit: SharedPreferences

    private val saveData = { value: String ->
        edit.edit().putString("userTag", value).apply()
    }

    private val getData = {
        edit.getString("userTag", null)
    }

    private val refreshUserData = {
        val userTag = binding.edUserTag.text.toString().trim()
        if (!TextUtils.isEmpty(userTag)) {
            binding.swp.isRefreshing = true
            if (chestHelper == null || chestHelper?.userTAG != userTag) {
                chestHelper = ClashRoyaleChestHelper(this, userTag)
            }
            Toast.makeText(this, "正在更新用户资料", Toast.LENGTH_LONG).show()
            chestHelper!!.refreshData { status, refreshResult ->
                mHandler.post {
                    // 进入回调函数就说明信息刷新完毕, 就可以取消刷新球了
                    binding.swp.isRefreshing = false
                    if (status == ClashRoyaleChestHelper.STATUS_SUCCESS) {
                        when {
                            refreshResult!!.status -> {
                                Toast.makeText(
                                    this,
                                    "更新用户资料成功！\n${refreshResult.msg} \n上次更新时间为 ${refreshResult.lastUpdateDate}",
                                    Toast.LENGTH_LONG
                                ).show()
                                edit.edit()
                                    .putString("lastRefreshTime", refreshResult.currentDate)
                                    .apply()
                                // binding.start.callOnClick()
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

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        edit = getSharedPreferences("saves", MODE_PRIVATE)
        if (getData() != null) {
            binding.edUserTag.setText(getData())
        }
        // 设置下拉刷新监听器，下拉刷新时更新用户信息 以及宝箱信息
        binding.swp.setOnRefreshListener(refreshUserData)
        // 设置布局管理器为网格布局，实现一行显示两个宝箱信息
        binding.chestList.layoutManager =
            GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)
        // 设置按键监听事件，点击 查宝箱 按钮时开始查宝箱
        binding.start.setOnClickListener {
            val userTag = binding.edUserTag.text.toString().trim()
            if (!TextUtils.isEmpty(userTag)) {
                saveData(userTag)
                // 重置 actionBar 的内容
                supportActionBar?.title = getString(R.string.app_name)
                // 开始查宝箱之前显示下拉刷新球
                binding.swp.isRefreshing = true
                // 点击按钮后禁用按钮，防止多次点击
                binding.start.isEnabled = false
                Toast.makeText(this, "正在获取宝箱信息", Toast.LENGTH_LONG).show()
                if (chestHelper == null || chestHelper?.userTAG != userTag) {
                    chestHelper = ClashRoyaleChestHelper(this, userTag)
                }
                chestHelper!!.getChestData { status, userName, chestData ->
                    when (status) {
                        ClashRoyaleChestHelper.STATUS_SUCCESS -> {
                            mHandler.post {
                                Toast.makeText(this, "宝箱信息获取成功！", Toast.LENGTH_SHORT).show()
                                binding.chestList.adapter = ChestAdapter(this, chestData!!)
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
                        binding.swp.isRefreshing = false
                        binding.start.isEnabled = true
                    }
                }
            } else {
                Toast.makeText(this, "请输入用户标签！", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuRefresh -> {
                refreshUserData()
            }
            R.id.menuAbout -> {
                val lastUpdate = edit.getString("lastRefreshTime", "null")
                AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("关于")
                    .setMessage("上次更新用户资料时间为 $lastUpdate")
                    .setPositiveButton("确定") { _, _ -> }
                    .create()
                    .show()
                Toast.makeText(this, "信息来源于网络！！", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }
}