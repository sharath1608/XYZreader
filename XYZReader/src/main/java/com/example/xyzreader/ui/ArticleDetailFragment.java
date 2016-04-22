package com.example.xyzreader.ui;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;
    private String mTitleText;
    private String mTitle;
    private int mTopInset;
    public ImageView mPhotoView;
    private View mPhotoContainerView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;
    private static final float PARALLAX_FACTOR = 1.25f;
    private ShareActionProvider mShareActionProvider;
    private CollapsingToolbarLayout collapsibleLayout;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
     //   mDrawInsetsFrameLayout = (DrawInsetsFrameLayout)
    //            mRootView.findViewById(R.id.draw_insets_frame_layout);
        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
//        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
//            @Override
//            public void onInsetsChanged(Rect insets) {
//                mTopInset = insets.top;
//            }
//        });

        collapsibleLayout = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsible_layout);
        collapsibleLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        collapsibleLayout.setVisibility(View.INVISIBLE);
        collapsibleLayout.setExpandedTitleTextAppearance(R.style.HeaderTheme);
        collapsibleLayout.setCollapsedTitleTextAppearance(R.style.HeaderTheme);

        mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);
        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollY = mScrollView.getScrollY();
                if(mScrollY!=0){
                    ((ArticleDetailActivity)getActivity()).mUpButtonContainer.setVisibility(View.INVISIBLE);
                }else{
                    ((ArticleDetailActivity)getActivity()).mUpButtonContainer.setVisibility(View.VISIBLE);
                }
                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
                mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                //updateStatusBar();
            }
        });

        mStatusBarColorDrawable = new ColorDrawable(0);
        bindViews();
        //updateStatusBar();
        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem shareItem;
        super.onCreateOptionsMenu(menu, inflater);
        shareItem = menu.findItem(R.id.article_share_item);
        mShareActionProvider = (ShareActionProvider)MenuItemCompat.getActionProvider(shareItem);
        mShareActionProvider.setShareIntent(createShareIntent());
    }

    public Intent createShareIntent(){
        Intent shareIntent;
        shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
        shareIntent.setType("text/plain");
        return shareIntent;
    }

    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        //mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        //TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);


        TextView authorView= (TextView) mRootView.findViewById(R.id.article_author);
        TextView dateView= (TextView) mRootView.findViewById(R.id.article_dateView);
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);
        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitleText = mCursor.getString(ArticleLoader.Query.TITLE);
            String authorString = mCursor.getString(ArticleLoader.Query.AUTHOR);
            String dateString = DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString();

            mTitle = mTitleText;
            authorView.setText(authorString);
            mPhotoView.setTransitionName(mTitleText);
            dateView.setText(dateString);
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {


                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                mPhotoView.setImageBitmap(bitmap);
                                mPhotoView.setScaleType(ImageView.ScaleType.FIT_XY);
                               // updateStatusBar();
                                collapsibleLayout.setVisibility(View.VISIBLE);
                                resumeSharedTransition();
                                collapsibleLayout.setTitle(mTitle);
                            }

                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }

                    });
        } else {
            mRootView.setVisibility(View.GONE);
            collapsibleLayout.setTitle("N/A");
            authorView.setText("N/A" );
            dateView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    public void resumeSharedTransition(){
        final ViewTreeObserver observer = mRootView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                getActivity().startPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    // Create a blur background of the foreground image
    public void blurBackgroundImage(ImageView view,Bitmap bitmap){
        Bitmap U8_4Bitmap;

        // Covert to a compatible RGB format if not already so.
        if(bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
            U8_4Bitmap = bitmap;
        } else {
            U8_4Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        Bitmap output = Bitmap.createBitmap(U8_4Bitmap.getWidth(), U8_4Bitmap.getHeight(), U8_4Bitmap.getConfig());
        RenderScript rs = RenderScript.create(getActivity().getApplicationContext());
        ScriptIntrinsicBlur theIntrinsic =  ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn =  Allocation.createFromBitmap(rs, U8_4Bitmap,Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_GRAPHICS_TEXTURE);
        Allocation tmpOut = Allocation.createFromBitmap(rs,output);
        theIntrinsic.setRadius(25.f);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(output);
        rs.destroy();
        Drawable drawable = new BitmapDrawable(getResources(),output);
        view.setAlpha(255);
        view.setBackground(drawable);
    }



    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }
}
