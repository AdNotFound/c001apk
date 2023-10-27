package com.example.c001apk.ui.fragment.feed.total

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ThemeUtils
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.c001apk.R
import com.example.c001apk.constant.Constants
import com.example.c001apk.databinding.DialogReplyToReplyBottomSheetBinding
import com.example.c001apk.logic.model.CheckResponse
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.ui.fragment.feed.IOnReplyClickListener
import com.example.c001apk.util.CookieUtil
import com.example.c001apk.util.LinearItemDecoration
import com.example.c001apk.util.PrefManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URLDecoder
import kotlin.concurrent.thread

class Reply2ReplyBottomSheetDialog : BottomSheetDialogFragment(), IOnReplyClickListener {

    private lateinit var binding: DialogReplyToReplyBottomSheetBinding
    private val viewModel by lazy { ViewModelProvider(this)[ReplyTotalViewModel::class.java] }
    private lateinit var id: String
    private lateinit var uid: String
    private var position: Int = 0
    private lateinit var mAdapter: Reply2ReplyTotalAdapter
    private lateinit var mLayoutManager: LinearLayoutManager
    private var lastVisibleItemPosition = -1
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var type = ""
    private var uname = ""
    private var ruid = ""
    private var rid = ""
    private var rPosition = 0
    private var r2rPosition = 0

    companion object {
        fun newInstance(position: Int, uid: String, id: String): Reply2ReplyBottomSheetDialog {
            val args = Bundle()
            args.putString("UID", uid)
            args.putString("ID", id)
            args.putInt("POSITION", position)
            val fragment = Reply2ReplyBottomSheetDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private fun setData() {
        val args = arguments
        id = args!!.getString("ID", "")
        uid = args.getString("UID", "")
        position = args.getInt("POSITION")
    }

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog)
    }*/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogReplyToReplyBottomSheetBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setData()
        initView()
        initData()
        initScroll()

        viewModel.replyTotalLiveData.observe(viewLifecycleOwner) { result ->
            val data = result.getOrNull()
            if (!data.isNullOrEmpty()) {
                if (!viewModel.isLoadMore)
                    viewModel.replyTotalList.clear()
                for (element in data)
                    if (element.entityType == "feed_reply")
                        viewModel.replyTotalList.add(element)
                mAdapter.notifyDataSetChanged()
                binding.indicator.isIndeterminate = false
                mAdapter.setLoadState(mAdapter.LOADING_COMPLETE)
            } else {
                mAdapter.setLoadState(mAdapter.LOADING_END)
                viewModel.isEnd = true
                result.exceptionOrNull()?.printStackTrace()
            }
        }

    }

    private fun initScroll() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (lastVisibleItemPosition == viewModel.replyTotalList.size) {
                        if (!viewModel.isEnd) {
                            mAdapter.setLoadState(mAdapter.LOADING)
                            viewModel.isLoadMore = true
                            viewModel.page++
                            viewModel.getReplyTotal()
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (viewModel.replyTotalList.isNotEmpty())
                    lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition()
            }
        })
    }

    private fun initData() {
        viewModel.id = id
        if (viewModel.replyTotalList.isEmpty()) {
            viewModel.isEnd = false
            viewModel.isLoadMore = false
            viewModel.getReplyTotal()
        }
    }

    private fun initView() {
        val space = resources.getDimensionPixelSize(R.dimen.normal_space)

        mAdapter =
            Reply2ReplyTotalAdapter(requireActivity(), uid, position, viewModel.replyTotalList)
        mLayoutManager = LinearLayoutManager(activity)
        mAdapter.setIOnReplyClickListener(this)
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = mLayoutManager
            if (itemDecorationCount == 0)
                addItemDecoration(LinearItemDecoration(space))
        }
    }

    override fun onStart() {
        super.onStart()
        val view: FrameLayout =
            dialog?.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!
        view.layoutParams.height = -1
        view.layoutParams.width = -1
    }

    override fun onReply2Reply(
        rPosition: Int,
        r2rPosition: Int?,
        id: String,
        uid: String,
        uname: String,
        type: String
    ) {
        if (PrefManager.isLogin) {
            this.rPosition = rPosition
            r2rPosition?.let { this.r2rPosition = r2rPosition }
            this.rid = id
            this.ruid = uid
            this.uname = uname
            this.type = type
            initReply()
        }
    }

    @SuppressLint("InflateParams")
    private fun initReply() {

        bottomSheetDialog = BottomSheetDialog(requireActivity())
        val view = LayoutInflater.from(requireActivity())
            .inflate(R.layout.dialog_reply_bottom_sheet, null, false)
        val editText: EditText = view.findViewById(R.id.editText)
        val publish: TextView = view.findViewById(R.id.publish)

        bottomSheetDialog.apply {
            setContentView(view)
            setCancelable(false)
            setCanceledOnTouchOutside(true)
            show()
            window?.apply {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        editText.hint = "回复: $uname"
        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        editText.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, 0)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            @SuppressLint("RestrictedApi")
            override fun afterTextChanged(p0: Editable?) {
                if (editText.text.toString().replace("\n", "").isEmpty()) {
                    publish.isClickable = false
                    publish.setTextColor(requireActivity().getColor(R.color.gray_bd))
                } else {
                    publish.isClickable = true
                    publish.setTextColor(
                        ThemeUtils.getThemeAttrColor(
                            requireActivity(),
                            com.drakeet.about.R.attr.colorPrimary
                        )
                    )
                    publish.setOnClickListener {
                        publish(editText.text.toString())
                    }
                }
            }
        })
    }

    private fun publish(content: String) {
        thread {
            try {
                val httpClient = OkHttpClient()
                val formBody: RequestBody = FormBody.Builder()
                    .add("message", content)
                    .build()

                val getRequest: Request = Request.Builder()
                    .addHeader("User-Agent", Constants.USER_AGENT)
                    .addHeader("X-Requested-With", Constants.REQUEST_WIDTH)
                    .addHeader("X-Sdk-Int", "33")
                    .addHeader("X-Sdk-Locale", "zh-CN")
                    .addHeader("X-App-Id", Constants.APP_ID)
                    .addHeader("X-App-Token", CookieUtil.token)
                    .addHeader("X-App-Version", "13.3.1")
                    .addHeader("X-App-Code", "2307121")
                    .addHeader("X-Api-Version", "13")
                    .addHeader("X-App-Device", CookieUtil.deviceCode)
                    .addHeader("X-Dark-Mode", "0")
                    .addHeader("X-App-Channel", "coolapk")
                    .addHeader("X-App-Mode", "universal")
                    .addHeader(
                        "Cookie",
                        "uid=${PrefManager.uid}; username=${PrefManager.username}; token=${PrefManager.token}"
                    )
                    .url("https://api.coolapk.com/v6/feed/reply?id=$rid&type=$type")
                    .post(formBody)
                    .build()

                val call: Call = httpClient.newCall(getRequest)

                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.d("Reply", "onFailure: ${e.message}")
                    }

                    @SuppressLint("NotifyDataSetChanged")
                    override fun onResponse(call: Call, response: Response) {
                        val reply: CheckResponse = Gson().fromJson(
                            response.body!!.string(),
                            CheckResponse::class.java
                        )
                        if (reply.data?.messageStatus == 1) {
                            requireActivity().runOnUiThread {
                                viewModel.replyTotalList.add(
                                    r2rPosition + 1,
                                    TotalReplyResponse.Data(
                                        "feed_reply",
                                        id,
                                        ruid,
                                        PrefManager.uid,
                                        URLDecoder.decode(PrefManager.username, "UTF-8"),
                                        uname,
                                        content,
                                        "",
                                        null,
                                        (System.currentTimeMillis() / 1000).toString(),
                                        "0",
                                        "0",
                                        PrefManager.userAvatar,
                                        null,
                                        0
                                    )
                                )
                                mAdapter.notifyItemInserted(r2rPosition + 1)
                                Toast.makeText(activity, "回复成功", Toast.LENGTH_SHORT).show()
                                bottomSheetDialog.cancel()
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                Toast.makeText(activity, reply.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}