package com.example.c001apk.ui.activity.user

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.ThemeUtils
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.c001apk.R
import com.example.c001apk.databinding.ActivityUserBinding
import com.example.c001apk.ui.activity.BaseActivity
import com.example.c001apk.ui.activity.fff.FFFListActivity
import com.example.c001apk.ui.fragment.home.feed.HomeFeedAdapter
import com.example.c001apk.ui.fragment.home.feed.IOnLikeClickListener
import com.example.c001apk.util.CountUtil
import com.example.c001apk.util.ImageShowUtil
import com.example.c001apk.util.LinearItemDecoration
import com.example.c001apk.util.PubDateUtil


class UserActivity : BaseActivity(), IOnLikeClickListener {

    private lateinit var binding: ActivityUserBinding
    private val viewModel by lazy { ViewModelProvider(this)[UserViewModel::class.java] }
    private lateinit var mAdapter: HomeFeedAdapter
    private lateinit var mLayoutManager: LinearLayoutManager

    @SuppressLint(
        "ResourceAsColor", "SetTextI18n", "NotifyDataSetChanged", "UseCompatLoadingForDrawables",
        "RestrictedApi", "ResourceType"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        initView()
        initData()
        initRefresh()
        initScroll()

        viewModel.userData.observe(this) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val user = result.getOrNull()
                if (user != null) {
                    binding.collapsingToolbar.title = user.username
                    binding.collapsingToolbar.setCollapsedTitleTextColor(this.getColor(R.color.white))
                    binding.collapsingToolbar.setExpandedTitleColor(this.getColor(com.google.android.material.R.color.mtrl_btn_transparent_bg_color))
                    ImageShowUtil.showUserCover(binding.cover, user.cover)
                    ImageShowUtil.showAvatar(binding.avatar, user.userAvatar)
                    binding.name.text = user.username
                    binding.level.text = "Lv.${user.level}"
                    binding.level.visibility = View.VISIBLE
                    if (user.bio == "") binding.bio.visibility = View.GONE
                    else binding.bio.text = user.bio
                    binding.like.text = "${CountUtil.view(user.beLikeNum)} 获赞"
                    binding.follow.text = "${CountUtil.view(user.follow)} 关注"
                    binding.fans.text = "${CountUtil.view(user.fans)} 粉丝"
                    binding.loginTime.text = PubDateUtil.time(user.logintime) + "活跃"

                    val intent = Intent(this, FFFListActivity::class.java)
                    intent.putExtra("uid", user.uid)
                    binding.follow.setOnClickListener {
                        intent.putExtra("type", "follow")
                        startActivity(intent)
                    }
                    binding.fans.setOnClickListener {
                        intent.putExtra("type", "fans")
                        startActivity(intent)
                    }

                    viewModel.uid = user.uid
                    viewModel.isRefreh = true
                    viewModel.isNew = true
                    viewModel.getUserFeed()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.userFeedData.observe(this) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val feed = result.getOrNull()
                if (!feed.isNullOrEmpty()) {
                    if (viewModel.isRefreh) viewModel.feedContentList.clear()
                    if (viewModel.isRefreh || viewModel.isLoadMore) {
                        for (element in feed) {
                            if (element.entityTemplate == "feed")
                                viewModel.feedContentList.add(element)
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                    mAdapter.setLoadState(mAdapter.LOADING_COMPLETE)
                } else {
                    mAdapter.setLoadState(mAdapter.LOADING_END)
                    viewModel.isEnd = true
                    result.exceptionOrNull()?.printStackTrace()
                }
                binding.infoLayout.visibility = View.VISIBLE
                binding.indicator.isIndeterminate = false
                binding.indicator.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                viewModel.isRefreh = false
            }
        }

        viewModel.likeFeedData.observe(this) { result ->
            if (viewModel.isPostLikeFeed) {
                viewModel.isPostLikeFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.feedContentList[viewModel.likePosition].likenum =
                            response.data.count
                        viewModel.feedContentList[viewModel.likePosition].userAction?.like = 1
                        mAdapter.notifyDataSetChanged()
                    } else
                        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.unLikeFeedData.observe(this) { result ->
            if (viewModel.isPostUnLikeFeed) {
                viewModel.isPostUnLikeFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.feedContentList[viewModel.likePosition].likenum =
                            response.data.count
                        viewModel.feedContentList[viewModel.likePosition].userAction?.like = 0
                        mAdapter.notifyDataSetChanged()
                    } else
                        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

    }

    private fun initScroll() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (viewModel.lastVisibleItemPosition == viewModel.feedContentList.size
                        && !viewModel.isEnd
                    ) {
                        mAdapter.setLoadState(mAdapter.LOADING)
                        viewModel.isLoadMore = true
                        viewModel.page++
                        viewModel.isNew = true
                        viewModel.getUserFeed()
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (viewModel.feedContentList.isNotEmpty()) {
                    viewModel.lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition()
                    viewModel.firstCompletelyVisibleItemPosition =
                        mLayoutManager.findFirstCompletelyVisibleItemPosition()
                }
            }
        })
    }

    @SuppressLint("RestrictedApi")
    private fun initRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ThemeUtils.getThemeAttrColor(
                this, rikka.preference.simplemenu.R.attr.colorPrimary
            )
        )
        binding.swipeRefresh.setOnRefreshListener {
            binding.indicator.isIndeterminate = false
            binding.indicator.visibility = View.GONE
            refreshData()
        }
    }

    private fun initData() {
        if (viewModel.isInit) {
            viewModel.isInit = false
            binding.indicator.visibility = View.VISIBLE
            binding.indicator.isIndeterminate = true
            refreshData()
        }
    }

    private fun refreshData() {
        viewModel.page = 1
        viewModel.isRefreh = true
        viewModel.isEnd = false
        viewModel.id = intent.getStringExtra("id")!!
        viewModel.isNew = true
        viewModel.getUser()
    }

    private fun initView() {
        val space = resources.getDimensionPixelSize(R.dimen.normal_space)
        mAdapter = HomeFeedAdapter(this, viewModel.feedContentList)
        mAdapter.setIOnLikeReplyListener(this)
        mLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = mLayoutManager
            if (itemDecorationCount == 0) addItemDecoration(LinearItemDecoration(space))
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    override fun onPostLike(isLike: Boolean, id: String, position: Int) {
        viewModel.likeFeedId = id
        viewModel.likePosition = position
        if (isLike) {
            viewModel.isPostUnLikeFeed = true
            viewModel.postUnLikeFeed()
        } else {
            viewModel.isPostLikeFeed = true
            viewModel.postLikeFeed()
        }
    }

}