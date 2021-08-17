package com.mrx.clashRoyal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.alibaba.fastjson.JSONObject
import okhttp3.*
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


class ClashRoyaleChestHelper(val context: Context, val userTAG: String) {
    companion object {
        private const val mainURL = "https://statsroyale.com/profile/"
        const val STATUS_SUCCESS = 1
        const val STATUS_FAILURE = 0
        const val STATUS_FAILURE_OTHER_ERROR = -1
        const val STATUS_FAILURE_USER_NOT_EXISTS = -2
        const val STATUS_FAILURE_UNKNOWN_CHEST = -3
        const val STATUS_FAILURE_GET_CHEST_PIC_TIMEOUT = -4
    }

    private val sdf = SimpleDateFormat("yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒", Locale.CHINA)
    private val profileURL = "$mainURL${userTAG.substringAfter("#")}"
    private val profileRefreshURL = "$profileURL/refresh"
    val okClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .dns(object : Dns {
            private val ipList = HashMap<String, MutableList<InetAddress>>()

            init {
                ipList["cdn.statsroyale.com"] = mutableListOf(
                    InetAddress.getByName("104.21.233.145"),
                    InetAddress.getByName("104.21.233.146")
                )
                ipList["statsroyale.com"] = mutableListOf(
                    InetAddress.getByName("34.117.221.221")
                )
            }

            override fun lookup(hostname: String): List<InetAddress> {

                return ipList[hostname]!!.apply {
                    add(InetAddress.getByName(hostname))
                }
            }
        })
        .build()
    val request = Request.Builder()
        .get()
        .url(profileURL)
        .addHeader(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        )
        .addHeader(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36"
        )
        .build()

    inner class Chest(
        val chestName: String,
        private val chestIMGUrl: String,
        val chestNum: String
    ) {
        var chestIMG: Bitmap

        init {
            println("获取宝箱图片 -> $chestIMGUrl")
            val files = context.filesDir.listFiles()
            val chestIMGFile = File(context.filesDir, chestName)
            if (files != null && chestIMGFile !in files) {
                println("未命中缓存, 缓存文件")
                val imgByteArray = okClient.newCall(request.newBuilder().url(chestIMGUrl).build())
                    .execute().body!!.bytes()
                chestIMG = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.size)
                chestIMGFile.writeBytes(imgByteArray)
            } else {
                println("命中缓存, 从文件中读取")
                chestIMG = BitmapFactory.decodeFile(chestIMGFile.absolutePath)
            }
            chestIMG = chestIMG.scale(200, 200)
        }

        override fun toString(): String {
            return "宝箱名 $chestNum $chestName 图标链接 $chestIMGUrl"
        }
    }

    inner class RefreshData(val status: Boolean, val msg: String, val lastUpdateDate: String) {
        val currentDate: String = sdf.format(Date())
    }

    private interface Translate {
        val chineseName: String
        val imgURL: String
    }

    private enum class ChestNameToChinese : Translate {

        SilverChest {
            override val chineseName = "白银宝箱"
            override val imgURL = "https://cdn.statsroyale.com/images/silver-chest.png"
        },
        GoldenChest {
            override val chineseName = "黄金宝箱"
            override val imgURL = "https://cdn.statsroyale.com/images/golden-chest.png"
        },
        MagicalChest {
            override val chineseName = "神奇宝箱"
            override val imgURL = "https://cdn.statsroyale.com/images/magical-chest.png"
        },
        GiantChest {
            override val chineseName = "巨型宝箱"
            override val imgURL = "https://cdn.statsroyale.com/images/giant-chest.png"
        },
        MegaLightningChest {
            override val chineseName = "超级雷电宝箱"
            override val imgURL = "https://cdn.statsroyale.com/images/super-lightning-chest.png"
        },
        EpicChest {
            override val chineseName = "史诗宝箱"
            override val imgURL = "https://cdn.statsroyale.com/images/epic-chest.png"
        },
        LegendaryChest {
            override val chineseName = "传奇宝箱"
            override val imgURL = "https://cdn.statsroyale.com/images/legendary-chest.png"
        },
        OverflowingGoldCrate {
            override val chineseName = "满溢金币箱"
            override val imgURL = "https://cdn.statsroyale.com/images/chests/OverflowingCrate.png"
        },
        GoldCrate {
            override val chineseName = "普通金币箱"
            override val imgURL = "https://cdn.statsroyale.com/images/chests/GoldCrate.png"
        },
        PlentifulGoldCrate {
            override val chineseName = "丰厚金币箱"
            override val imgURL = "https://cdn.statsroyale.com/images/chests/PlentifulCrate.png"
        }

    }

    fun getChestData(callback: (STATUS: Int, userName: String?, chestList: LinkedList<Chest>?) -> Unit) {
        println("正在请求数据！")
        okClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(STATUS_FAILURE, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.apply {
                    val soup = Jsoup.parse(this.string())
                    val userName = soup.title().substringBefore("'s")
                    val chests =
                        soup.select("body > div.layout__page > div.layout__container > div > div.chests.profile__chests > div.chests__queue > div")
                    val chestList = LinkedList<Chest>()
                    for (chest in chests) {
                        val chestText = chest.select("div > div").first()!!.text()
                        val chestName = chestText.substringAfter(":").trim().replace(" ", "")
                        val chestNameEnum: ChestNameToChinese
                        try {
                            chestNameEnum = ChestNameToChinese.valueOf(chestName)
                        } catch (e: IllegalArgumentException) {
                            callback(STATUS_FAILURE_UNKNOWN_CHEST, null, null)
                            return
                        }
                        val chestNum = if (":" in chestText) {
                            chestText.substringBefore(":").trim()
                        } else {
                            "+0"
                        }
                        println(chestText)
                        val chestIMGURL = chestNameEnum.imgURL
                        val chineseChestName = chestNameEnum.chineseName
                        try {
                            val chestObj = Chest(chineseChestName, chestIMGURL, chestNum)
                            chestList.add(chestObj)
                        } catch (e: SocketTimeoutException) {
                            callback(STATUS_FAILURE_GET_CHEST_PIC_TIMEOUT, null, null)
                            return
                        }
                    }

                    // 如果获取不到宝箱信息就表示用户信息出错， 否则就返回宝箱信息
                    if (chestList.size == 0) {
                        callback(STATUS_FAILURE_USER_NOT_EXISTS, null, null)
                    } else {
                        callback(STATUS_SUCCESS, userName, chestList)
                    }
                    return
                }
                callback(STATUS_FAILURE_OTHER_ERROR, null, null)
            }
        })

    }

    fun refreshData(callback: (STATUS: Int, refreshData: ClashRoyaleChestHelper.RefreshData?) -> Unit) {
        okClient.newCall(request.newBuilder().url(profileRefreshURL).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback(STATUS_FAILURE_OTHER_ERROR, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body
                    if (responseBody == null) {
                        callback(STATUS_FAILURE_OTHER_ERROR, null)
                        return
                    }

                    val jsonObj = JSONObject.parseObject(responseBody.string())
                    if (jsonObj["lastupdate"] == null) {
                        callback(STATUS_FAILURE_USER_NOT_EXISTS, null)
                        return
                    }
                    val status = jsonObj["success"] as Boolean
                    val message = jsonObj["message"] as String
                    val lastUpdate =
                        sdf.format(Date((jsonObj["lastupdate"] as Int).toLong() * 1000))
                    callback(STATUS_SUCCESS, RefreshData(status, message, lastUpdate))
                }
            })

    }
}