package tw.nekomimi.nekogram.utils

import android.content.Context

object UpdateUtil {

    const val channelUsername = ""
    const val channelUsernameTips = ""
    const val wikiUrl = "https://github.com/Xlufoi/NyashkaGram"

    @JvmStatic
    fun getChannelUrl(): String {
        return "https://github.com/Xlufoi/NyashkaGram"
    }

    @JvmStatic
    fun getTipsUrl(): String {
        return "https://github.com/Xlufoi/NyashkaGram"
    }

    @JvmStatic
    fun postCheckFollowChannel(ctx: Context, currentAccount: Int) {
        // Disabled in NyashkaGram
    }

    @JvmStatic
    fun postCheckFollowTipsChannel(ctx: Context, currentAccount: Int) {
        // Disabled in NyashkaGram
    }
}
