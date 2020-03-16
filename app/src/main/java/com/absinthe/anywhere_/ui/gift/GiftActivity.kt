package com.absinthe.anywhere_.ui.gift

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.absinthe.anywhere_.BaseActivity
import com.absinthe.anywhere_.BuildConfig
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.adapter.gift.ChatAdapter
import com.absinthe.anywhere_.adapter.gift.LeftChatNode
import com.absinthe.anywhere_.adapter.manager.SmoothScrollLayoutManager
import com.absinthe.anywhere_.databinding.ActivityGiftBinding
import com.absinthe.anywhere_.model.GiftChatString
import com.absinthe.anywhere_.utils.AppUtils
import com.absinthe.anywhere_.utils.manager.DialogManager
import com.absinthe.anywhere_.viewmodel.GiftViewModel
import com.chad.library.adapter.base.entity.node.BaseNode
import timber.log.Timber
import java.util.*

class GiftActivity : BaseActivity() {

    private lateinit var mBinding: ActivityGiftBinding
    private var mAdapter: ChatAdapter = ChatAdapter()

    override fun setViewBinding() {
        mBinding = ActivityGiftBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }

    override fun setToolbar() {
        mToolbar = mBinding.toolbar.toolbar
    }

    override fun isPaddingToolbar(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        if (!BuildConfig.DEBUG) {
            finish()
        }
    }

    override fun onDestroy() {
        instance = null
        mViewModel!!.stopOffer()
        mViewModel = null
        super.onDestroy()
    }

    override fun initView() {
        super.initView()
        if (mViewModel == null) {
            mViewModel = ViewModelProvider(this).get(GiftViewModel::class.java)
        }

        mBinding.rvChat.adapter = mAdapter
        val manager = SmoothScrollLayoutManager(this)
        mBinding.rvChat.layoutManager = manager
        mBinding.rvChat.setHasFixedSize(true)
        mBinding.rvChat.isNestedScrollingEnabled = false

        mBinding.ibSend.setOnClickListener {
            val content = mBinding.etChat.text.toString()
            if (!TextUtils.isEmpty(content)) {
                if (com.absinthe.anywhere_.utils.TextUtils.isGiftCode(content)) {
                    mViewModel!!.getCode(content)
                } else {
                    mViewModel!!.responseChat()
                }
                mViewModel!!.addChat(content, ChatAdapter.TYPE_RIGHT)
                mBinding.etChat.setText("")
            }
        }
        mViewModel!!.node.observe(this, Observer { node: BaseNode? ->
            if (node is LeftChatNode) {
                mBinding.toolbar.toolbar.setTitle(R.string.settings_gift_typing)
                val delay = Random().nextInt(500) + 1000
                Handler(Looper.getMainLooper()).postDelayed({
                    mAdapter.addData(node)
                    try {
                        Thread.sleep(50)
                        mBinding.rvChat.smoothScrollToPosition(mAdapter.itemCount - 1)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    mBinding.toolbar.toolbar.setTitle(R.string.settings_gift)
                }, delay.toLong())
            } else {
                mAdapter.addData(node!!)
                try {
                    Thread.sleep(50)
                    mBinding.rvChat.smoothScrollToPosition(mAdapter.itemCount - 1)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        })
        mViewModel!!.price
        mViewModel!!.chatQueue.offer(GiftChatString.chats)
        Timber.d(AppUtils.getAndroidId(this))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gift_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.toolbar_gift) {
            DialogManager.showGiftPriceDialog(this)
        }
        return super.onOptionsItemSelected(menuItem)
    }

    companion object {
        var instance: GiftActivity? = null
            private set
        private var mViewModel: GiftViewModel? = null
    }
}