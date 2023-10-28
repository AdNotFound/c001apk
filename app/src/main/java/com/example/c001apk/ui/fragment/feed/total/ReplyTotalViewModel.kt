package com.example.c001apk.ui.fragment.feed.total

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.logic.network.Repository

class ReplyTotalViewModel : ViewModel() {

    var replyTextMap: MutableMap<String, String> = HashMap()

    var isEnd = false
    var isLoadMore = false

    var page = 1
    var id = ""

    val replyTotalList = ArrayList<TotalReplyResponse.Data>()

    private val getReplyTotalLiveData = MutableLiveData<String>()

    val replyTotalLiveData = getReplyTotalLiveData.switchMap {
        Repository.getReply2Reply(id, page)
    }

    fun getReplyTotal() {
        getReplyTotalLiveData.value = getReplyTotalLiveData.value
    }

}