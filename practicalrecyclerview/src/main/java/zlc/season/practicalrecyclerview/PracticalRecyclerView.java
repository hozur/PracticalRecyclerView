package zlc.season.practicalrecyclerview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.example.practicalrecyclerview.R;

import java.util.Observable;
import java.util.Observer;


/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/9/21
 * Time: 13:56
 * FIXME
 */
public class PracticalRecyclerView extends FrameLayout {

    private DataSetObserver mObserver = new DataSetObserver();

    private FrameLayout mMain;
    private FrameLayout mLoading;
    private FrameLayout mError;
    private FrameLayout mEmpty;
    private LinearLayout mContent;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private View mLoadMoreView;
    private View mNoMoreView;
    private View mLoadMoreFailedView;

    private OnRefreshListener mRefreshListener;
    private OnLoadMoreListener mLoadMoreListener;

    private boolean isRefreshing = false;
    private boolean nowLoading = false;
    private boolean noMore = false;
    private boolean loadMoreFailed = false;
    private View mEmptyView;
    private View mLoadingView;
    private View mErrorView;

    public PracticalRecyclerView(Context context) {
        this(context, null);
    }

    public PracticalRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PracticalRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initMainView(context);
        obtainStyledAttributes(context, attrs);
        configDefaultBehavior();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PracticalRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initMainView(context);
        obtainStyledAttributes(context, attrs);
        configDefaultBehavior();
    }

    public void configureView(Configure configure) {
        if (configure == null) {
            return;
        }
        mErrorView.setOnClickListener(null);
        mLoadMoreFailedView.setOnClickListener(null);
        configure.configureEmptyView(mEmptyView);
        configure.configureErrorView(mErrorView);
        configure.configureLoadingView(mLoadingView);
        configure.configureLoadMoreView(mLoadMoreView);
        configure.configureNoMoreView(mNoMoreView);
        configure.configureLoadMoreFailedView(mLoadMoreFailedView);
    }

    public void setRefreshListener(OnRefreshListener refreshListener) {
        if (refreshListener == null) return;
        mSwipeRefreshLayout.setEnabled(true);
        mRefreshListener = refreshListener;
    }

    public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
        if (loadMoreListener == null) return;
        mLoadMoreListener = loadMoreListener;
    }

    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        mRecyclerView.setLayoutManager(layoutManager);
    }

    public void setAdapterWithLoading(RecyclerView.Adapter adapter) {
        if (adapter instanceof AbstractAdapter) {
            AbstractAdapter abstractAdapter = (AbstractAdapter) adapter;
            subscribeWithAdapter(abstractAdapter);
        }
        setLoadingVisibleAndResetStatus();
        mRecyclerView.setAdapter(adapter);
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        mRecyclerView.setAdapter(adapter);
    }

    public void setEmptyView(View emptyView) {
        mEmpty.removeAllViews();
        mEmpty.addView(emptyView);
    }

    public void setErrorView(View errorView) {
        mError.removeAllViews();
        mError.addView(errorView);
    }

    void setLoadingVisibleAndResetStatus() {
        mError.setVisibility(GONE);
        mContent.setVisibility(GONE);
        mEmpty.setVisibility(GONE);
        mLoading.setVisibility(VISIBLE);
        resetStatus();
    }

    void setContentVisibleAndResetStatus() {
        mLoading.setVisibility(GONE);
        mError.setVisibility(GONE);
        mEmpty.setVisibility(GONE);
        mContent.setVisibility(VISIBLE);
        resetStatus();
    }

    void setEmptyVisibleAndResetStatus() {
        mLoading.setVisibility(GONE);
        mContent.setVisibility(GONE);
        mError.setVisibility(GONE);
        mEmpty.setVisibility(VISIBLE);
        resetStatus();
    }

    void setErrorVisibleAndResetStatus() {
        mLoading.setVisibility(GONE);
        mContent.setVisibility(GONE);
        mEmpty.setVisibility(GONE);
        mError.setVisibility(VISIBLE);
        resetStatus();
    }

    void showNoMoreView() {
        if (!(mRecyclerView.getAdapter() instanceof AbstractAdapter)) return;
        noMore = true;
        AbstractAdapter adapter = (AbstractAdapter) mRecyclerView.getAdapter();
        adapter.show(mNoMoreView);
    }

    void showLoadMoreFailedView() {
        if (!(mRecyclerView.getAdapter() instanceof AbstractAdapter)) return;
        loadMoreFailed = true;
        AbstractAdapter adapter = (AbstractAdapter) mRecyclerView.getAdapter();
        adapter.show(mLoadMoreFailedView);
    }

    void resumeLoadMore() {
        loadMoreFailed = false;
        if (canNotLoadMore()) return;
        showLoadMoreView();
        mLoadMoreListener.onLoadMore();
    }

    void showLoadMoreView() {
        if (!(mRecyclerView.getAdapter() instanceof AbstractAdapter)) return;
        nowLoading = true;
        AbstractAdapter adapter = (AbstractAdapter) mRecyclerView.getAdapter();
        adapter.show(mLoadMoreView);
    }

    void autoLoadMore() {
        if (canNotLoadMore()) return;
        showLoadMoreView();
        mLoadMoreListener.onLoadMore();
    }

    private void resetStatus() {
        isRefreshing = false;
        nowLoading = false;
        loadMoreFailed = false;
        noMore = false;
    }

    private void initMainView(Context context) {
        mMain = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.recycler_layout, this, true);
        mLoading = (FrameLayout) mMain.findViewById(R.id.practical_loading);
        mError = (FrameLayout) mMain.findViewById(R.id.practical_error);
        mEmpty = (FrameLayout) mMain.findViewById(R.id.practical_empty);
        mContent = (LinearLayout) mMain.findViewById(R.id.practical_content);

        mSwipeRefreshLayout = (SwipeRefreshLayout) mMain.findViewById(R.id.practical_swipe_refresh);
        mRecyclerView = (RecyclerView) mMain.findViewById(R.id.practical_recycler);
    }

    private void configDefaultBehavior() {
        //默认为关闭,设置OnRefreshListener时打开
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        mRecyclerView.addOnScrollListener(new OnScrollListener());

        //设置error view 默认行为,点击刷新
        mErrorView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setLoadingVisibleAndResetStatus();
                refresh();
            }
        });
        //设置load more failed view 默认行为, 点击恢复加载
        mLoadMoreFailedView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resumeLoadMore();
            }
        });
    }

    private void refresh() {
        if (canNotRefresh()) {
            closeRefreshing();
            return;
        }
        mRefreshListener.onRefresh();
        isRefreshing = true;
    }

    private void closeRefreshing() {
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
            isRefreshing = false;
        }
    }

    private void closeLoadingMore() {
        if (nowLoading) {
            nowLoading = false;
        }
    }

    private boolean canNotRefresh() {
        return mRefreshListener == null || isRefreshing || nowLoading;
    }

    private boolean canNotLoadMore() {
        return mLoadMoreListener == null || isRefreshing || nowLoading || loadMoreFailed || noMore;
    }

    private void obtainStyledAttributes(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.PracticalRecyclerView);
        int loadingResId = attributes.getResourceId(R.styleable.PracticalRecyclerView_loading_layout, R.layout
                .default_loading_layout);
        int emptyResId = attributes.getResourceId(R.styleable.PracticalRecyclerView_empty_layout, R.layout
                .default_empty_layout);
        int errorResId = attributes.getResourceId(R.styleable.PracticalRecyclerView_error_layout, R.layout
                .default_error_layout);

        int loadMoreResId = attributes.getResourceId(R.styleable.PracticalRecyclerView_load_more_layout, R.layout
                .default_load_more_layout);
        int noMoreResId = attributes.getResourceId(R.styleable.PracticalRecyclerView_no_more_layout, R.layout
                .default_no_more_layout);
        int loadMoreErrorResId = attributes.getResourceId(R.styleable.PracticalRecyclerView_load_more_failed_layout, R
                .layout.default_load_more_failed_layout);

        mLoadingView = LayoutInflater.from(context).inflate(loadingResId, mLoading, true);
        mEmptyView = LayoutInflater.from(context).inflate(emptyResId, mEmpty, true);
        mErrorView = LayoutInflater.from(context).inflate(errorResId, mError, true);

        mLoadMoreView = LayoutInflater.from(context).inflate(loadMoreResId, mMain, false);
        mNoMoreView = LayoutInflater.from(context).inflate(noMoreResId, mMain, false);
        mLoadMoreFailedView = LayoutInflater.from(context).inflate(loadMoreErrorResId, mMain, false);

        attributes.recycle();
    }

    private void subscribeWithAdapter(AbstractAdapter adapter) {
        adapter.registerObserver(mObserver);
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    private class OnScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (canNotLoadMore()) return;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            int visibleItemCount = layoutManager.getChildCount();
            int totalItemCount = layoutManager.getItemCount();
            int lastVisibleItemPosition = getLastVisibleItemPosition(layoutManager);
            if (visibleItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 1 && totalItemCount >=
                    visibleItemCount) {
                showLoadMoreView();
                mLoadMoreListener.onLoadMore();
            }
        }

        private int getLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
            int lastVisibleItemPosition;
            if (layoutManager instanceof GridLayoutManager) {
                lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(into);
                lastVisibleItemPosition = findMax(into);
            } else {
                lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            }
            return lastVisibleItemPosition;
        }

        private int findMax(int[] lastPositions) {
            int max = lastPositions[0];
            for (int value : lastPositions) {
                if (value > max) {
                    max = value;
                }
            }
            return max;
        }
    }

    private class DataSetObserver implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            closeRefreshing();
            closeLoadingMore();
            Bridge type = (Bridge) arg;
            type.doSomething(PracticalRecyclerView.this);
        }
    }
}
