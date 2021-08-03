package com.mrx.clashRoyal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.alibaba.fastjson.JSONObject
import okhttp3.*
import org.jsoup.Jsoup
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ClashRoyaleChestHelper(val userTAG: String) {
    companion object {
        private const val mainURL = "https://statsroyale.com/profile/"
        const val STATUS_SUCCESS = 1
        const val STATUS_FAILURE = 0
        const val STATUS_FAILURE_USER_NOT_EXISTS = -1
    }

    private val sdf = SimpleDateFormat("yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒", Locale.CHINA)
    private val profileURL = "$mainURL${userTAG.substringAfter("#")}"
    private val profileRefreshURL = "$profileURL/refresh"
    val okClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
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
            val imgByteArray = okClient.newCall(request.newBuilder().url(chestIMGUrl).build())
                .execute().body!!.bytes()
            chestIMG = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.size)
            chestIMG = chestIMG.scale(200, 200)
        }

        override fun toString(): String {
            return "宝箱名 $chestNum $chestName 图标链接 $chestIMGUrl"
        }
    }

    class RefreshData(val status: Boolean, val msg: String, val lastUpdateDate: String)

    private interface Translate {
        fun getChinese(): String
        fun getIMGUrl(): String
    }

    private enum class ChestNameToChinese : Translate {

        SilverChest {
            override fun getChinese(): String {
                return "白银宝箱"
            }

            override fun getIMGUrl(): String {
                return "https://cdn.statsroyale.com/images/silver-chest.png"
            }
        },
        GoldenChest {
            override fun getChinese(): String {
                return "黄金宝箱"
            }

            override fun getIMGUrl(): String {
                return "https://cdn.statsroyale.com/images/golden-chest.png"
            }
        },
        MagicalChest {
            override fun getChinese(): String {
                return "神奇宝箱"
            }

            override fun getIMGUrl(): String {
                return "https://cdn.statsroyale.com/images/magical-chest.png"
            }
        },
        GiantChest {
            override fun getChinese(): String {
                return "巨型宝箱"
            }

            override fun getIMGUrl(): String {
                return "https://cdn.statsroyale.com/images/giant-chest.png"
            }
        },
        MegaLightningChest {
            override fun getChinese(): String {
                return "超级雷电宝箱"
            }

            override fun getIMGUrl(): String {
                return "https://cdn.statsroyale.com/images/super-lightning-chest.png"
            }
        },
        EpicChest {
            override fun getChinese(): String {
                return "史诗宝箱"
            }

            override fun getIMGUrl(): String {
                return "https://cdn.statsroyale.com/images/epic-chest.png"
            }
        },
        LegendaryChest {
            override fun getChinese(): String {
                return "传奇宝箱"
            }

            override fun getIMGUrl(): String {
                return "https://cdn.statsroyale.com/images/legendary-chest.png"
            }
        },
        OverflowingGoldCrate {
            override fun getChinese(): String {
                return "满溢金币箱"
            }

            override fun getIMGUrl(): String {
                return "https://cdn.statsroyale.com/images/chests/OverflowingCrate.png"
            }
        }
    }

    fun getChestData(callback: (STATUS: Int, userName: String?, chestList: LinkedList<Chest>?) -> Unit) {
        println("正在请求数据！")
        okClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(STATUS_FAILURE, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (responseBody == null) {
                    callback(STATUS_FAILURE, null, null)
                    return
                }

                val soup = Jsoup.parse(responseBody.string())
                val userName = soup.title().substringBefore("'s")
                val chests =
                    soup.select("body > div.layout__page > div.layout__container > div > div.chests.profile__chests > div.chests__queue > div")
                val chestList = LinkedList<Chest>()
                for (chest in chests) {
                    val chestText = chest.select("div > div").first()!!.text()
                    val chestName = chestText.substringAfter(":").trim().replace(" ", "")
                    val chestNameEnum = ChestNameToChinese.valueOf(chestName)
                    val chestNum = if (":" in chestText) {
                        chestText.substringBefore(":").trim()
                    } else {
                        "下一个"
                    }
                    println(chestText)
                    val chestIMGURL = chestNameEnum.getIMGUrl()
                    val chineseChestName = chestNameEnum.getChinese()
                    val chestObj = Chest(chineseChestName, chestIMGURL, chestNum)
                    chestList.add(chestObj)
                }
                if (chestList.size == 0) {
                    callback(STATUS_FAILURE_USER_NOT_EXISTS, "用户不存在", null)
                } else {
                    callback(STATUS_SUCCESS, userName, chestList)
                }
            }
        })

    }

    fun refreshData(callback: (STATUS: Int, refreshData: RefreshData?) -> Unit) {
        okClient.newCall(request.newBuilder().url(profileRefreshURL).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback(STATUS_FAILURE, null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body
                    if (responseBody == null) {
                        callback(STATUS_FAILURE, null)
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