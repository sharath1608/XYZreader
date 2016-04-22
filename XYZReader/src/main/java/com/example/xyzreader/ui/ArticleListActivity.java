package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Bundle sharedViewBundle;
    private View listItemView;
    private View mImageView;
    private String mArticleTitle;
    private int mSelectedPosition;
    private int mCurrentPosition;
    private final String EXTRA_CURRENT_POSITION = "current_pos";
    private ImageView sharedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.blue_A700)));
        View customView = getLayoutInflater().inflate(R.layout.custom_toolbar, null);
        actionBar.setCustomView(customView);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        Toolbar parent =(Toolbar) customView.getParent();
        parent.setContentInsetsAbsolute(0, 0);
        parent.setPadding(30, 0, 0, 0);
        actionBar.setTitle(R.string.action_bar_title);
        actionBar.setIcon(getDrawable(R.drawable.ic_launcher));
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.blue_A900));
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateRefreshingUI();
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        final SharedElementCallback mCallback = new SharedElementCallback() {

            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                super.onMapSharedElements(names, sharedElements);
                if(mSelectedPosition!=mCurrentPosition){
                    names.clear();
                    sharedElements.clear();
                    ViewHolder vh = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(mCurrentPosition);
                    if(vh!=null) {
                        sharedView = (ImageView)vh.itemView.findViewById(R.id.thumbnail);
                    }
                    if(sharedView!=null){
                        sharedElements.put(sharedView.getTransitionName(),sharedView);
                    }
                }
            }
        };

        if (savedInstanceState == null) {
            refresh();
        }
        setExitSharedElementCallback(mCallback);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        if(resultCode == RESULT_OK){
            mCurrentPosition = data.getIntExtra(EXTRA_CURRENT_POSITION, mSelectedPosition);
            if (mCurrentPosition != mSelectedPosition) {
                mRecyclerView.scrollToPosition(mCurrentPosition-2);
            }
            Log.i(this.getClass().getSimpleName(),String.valueOf(mCurrentPosition));
            postponeEnterTransition();
            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mRecyclerView.requestLayout();
                    startPostponedEnterTransition();
                    return true;
                }
            });
        }
    }


    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            listItemView = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(listItemView);
            listItemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View cardView) {
                    View sharedView = cardView.findViewById(R.id.thumbnail);
                    sharedViewBundle = ActivityOptions
                            .makeSceneTransitionAnimation(
                                    ArticleListActivity.this,
                                    sharedView,
                                    sharedView.getTransitionName()).toBundle();
                    mSelectedPosition = vh.getAdapterPosition();
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))), sharedViewBundle);
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            mArticleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            holder.titleView.setText(mArticleTitle);
            holder.authorView.setText(mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.dateView.setText(
                    DateUtils.getRelativeTimeSpanString(
                    mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString());
            mImageView = holder.thumbnailView;
            mImageView.setTransitionName(mArticleTitle);
            mImageView.setTag(mArticleTitle);
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            Picasso.with(getApplicationContext())
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .into(holder.thumbnailView);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView authorView;
        public TextView dateView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            authorView = (TextView) view.findViewById(R.id.article_author);
            dateView = (TextView) view.findViewById(R.id.article_dateView);
        }

    }
}
