package com.absinthe.anywhere_.ui.qrcode

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.Window
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.anywhere_.BaseActivity
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.adapter.SpacesItemDecoration
import com.absinthe.anywhere_.adapter.card.BaseCardAdapter
import com.absinthe.anywhere_.adapter.card.LAYOUT_MODE_STREAM
import com.absinthe.anywhere_.adapter.manager.WrapContentStaggeredGridLayoutManager
import com.absinthe.anywhere_.constants.OnceTag
import com.absinthe.anywhere_.databinding.ActivityQrcodeCollectionBinding
import com.absinthe.anywhere_.databinding.CardQrCollectionTipBinding
import com.absinthe.anywhere_.model.manager.QRCollection
import com.absinthe.anywhere_.utils.StatusBarUtil
import com.chad.library.adapter.base.BaseQuickAdapter
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QRCodeCollectionActivity : BaseActivity() {

    private lateinit var binding: ActivityQrcodeCollectionBinding
    private var mAdapter = BaseCardAdapter(LAYOUT_MODE_STREAM)

    init {
        isPaddingToolbar = true
    }

    override fun setViewBinding() {
        binding = ActivityQrcodeCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setToolbar() {
        mToolbar = binding.toolbar.toolbar
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementsUseOverlay = false
        }
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun initView() {
        super.initView()

        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.QR_COLLECTION_TIP)) {
            val tipBinding = CardQrCollectionTipBinding.inflate(
                    layoutInflater, binding.llContainer, false)

            binding.llContainer.addView(tipBinding.root, 0)

            tipBinding.btnOk.setOnClickListener {
                binding.llContainer.removeView(tipBinding.root)
                Once.markDone(OnceTag.QR_COLLECTION_TIP)
            }
        }
        binding.apply {
            recyclerView.apply {
                adapter = mAdapter
                layoutManager = WrapContentStaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
                addItemDecoration(SpacesItemDecoration(resources.getDimension(R.dimen.cardview_item_margin).toInt()))
                setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom + StatusBarUtil.getNavBarHeight())
            }
            srlQrCollection.isRefreshing = true
        }

        mAdapter.setOnItemClickListener { _: BaseQuickAdapter<*, *>?, view: View, position: Int -> mAdapter.clickItem(view, position) }
        mAdapter.setOnItemLongClickListener { _: BaseQuickAdapter<*, *>?, view: View, position: Int -> mAdapter.longClickItem(view, position) }

        lifecycleScope.launch(Dispatchers.Main) {
            val collection = QRCollection.Singleton.INSTANCE.instance
            mAdapter.setNewInstance(collection.list)

            binding.srlQrCollection.apply {
                isRefreshing = false
                isEnabled = false
            }
        }
    }
}