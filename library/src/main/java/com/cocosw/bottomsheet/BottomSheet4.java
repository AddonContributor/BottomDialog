package com.cocosw.bottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.*;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * @author Bramengton on 21/08/2017.
 */
public class BottomSheet4 extends BottomSheetDialogFragment implements DialogInterface {

    private Builder mBuilder;
    protected void add(Builder bilder){
        mBuilder = bilder;
    }

    private String moreText;
    private Drawable close;
    private Drawable more;
    private int mHeaderLayoutId;
    private int mListItemLayoutId;
    private int mGridItemLayoutId;

    private boolean collapseListIcons;
    private PinnedSectionGridView list;
    private SimpleSectionedGridAdapter adapter;

    private ImageView icon;

    private int limit = -1;

    private ActionMenu fullMenuItem;
    private ActionMenu menuItem;
    private ActionMenu actions;


    private static int getThemeResId(Context context, int themeId) {
        if (themeId == 0) {
            // If the provided theme is 0, then retrieve the dialogTheme from our theme
            TypedValue outValue = new TypedValue();
            if (context.getTheme().resolveAttribute(R.attr.bottomDialogStyle, outValue, true)) {
                Log.d("Resolve", "Attribute: found" );
                themeId = outValue.resourceId;
            }else{
                Log.d("Resolve", "Attribute: NOT found" );
                themeId = R.style.BottomSheet_Dialog;
            }
        }
        return themeId;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NO_TITLE, getThemeResId(getContext(), getTheme()));
        TypedArray a = this.getContext()
                .obtainStyledAttributes(null, R.styleable.BottomSheet, R.attr.bottomDialogStyle, 0);
        try {
            more = a.getDrawable(R.styleable.BottomSheet_bd_MoreDrawable);
            close = a.getDrawable(R.styleable.BottomSheet_bd_CloseDrawable);
            moreText = a.getString(R.styleable.BottomSheet_bd_MoreText);
            collapseListIcons = a.getBoolean(R.styleable.BottomSheet_bd_CollapseListIcons, true);
            mHeaderLayoutId = a.getResourceId(R.styleable.BottomSheet_bd_HeaderLayout, R.layout.bs_header);
            mListItemLayoutId = a.getResourceId(R.styleable.BottomSheet_bd_ListItemLayout, R.layout.bs_list_entry);
            mGridItemLayoutId = a.getResourceId(R.styleable.BottomSheet_bd_GridItemLayout, R.layout.bs_grid_entry);
        } finally {
            a.recycle();
        }
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        init(dialog);
    }

    /**
     * Hacky way to get gridview's column number
     */
    private int getNumColumns() {
        try {
            Field numColumns = GridView.class.getDeclaredField("mRequestedNumColumns");
            numColumns.setAccessible(true);
            return numColumns.getInt(list);
        } catch (Exception e) {
            return 1;
        }
    }

    private void init(final Dialog dialog) {
        final View mDialogView = dialog.getLayoutInflater().inflate(R.layout.bottom_sheet_dialog, null);
        LinearLayout header = (LinearLayout) dialog.getLayoutInflater().inflate(mHeaderLayoutId, null);

        LinearLayout mainLayout = (LinearLayout) mDialogView.findViewById(R.id.bs_main);
        mainLayout.addView(header, 0);

        final TextView title = (TextView) mDialogView.findViewById(R.id.bottom_sheet_title);
        if (mBuilder.title != null) {
            title.setVisibility(View.VISIBLE);
            title.setText(mBuilder.title);
        }

        icon = (ImageView) mDialogView.findViewById(R.id.bottom_sheet_title_image);
        list = (PinnedSectionGridView) mDialogView.findViewById(R.id.bottom_sheet_gridview);
        if (!mBuilder.grid) {
            list.setNumColumns(1);
        }

        if (mBuilder.grid) {
            for (int i = 0; i < getMenu().size(); i++) {
                if (getMenu().getItem(i).getIcon() == null)
                    throw new IllegalArgumentException("You must set icon for each items in grid style");
            }
        }

        if (mBuilder.limit > 0)
            limit = mBuilder.limit * getNumColumns();
        else
            limit = Integer.MAX_VALUE;


        actions = mBuilder.menu;
        menuItem = actions;
        // over the initial numbers
        if (getMenu().size() > limit) {
            fullMenuItem = mBuilder.menu;
            menuItem = mBuilder.menu.clone(limit - 1);
            ActionMenuItem item = new ActionMenuItem(getContext(), 0, R.id.bs_more, 0, limit - 1, moreText);
            item.setIcon(more);
            menuItem.add(item);
            actions = menuItem;
        }



        final int layout = mBuilder.grid ? mGridItemLayoutId : mListItemLayoutId;
        final ActionsAdapter baseAdapter = new ActionsAdapter(dialog, actions, layout, collapseListIcons);
        adapter = new SimpleSectionedGridAdapter(dialog, baseAdapter, R.layout.bs_list_divider, R.id.headerlayout, R.id.header);
        adapter.setGridView(list);


        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (((MenuItem) adapter.getItem(position)).getItemId() == R.id.bs_more) {
                    showFullItems();
                    return;
                }

                if (!((ActionMenuItem) adapter.getItem(position)).invoke()) {
                    if (mBuilder.menulistener != null)
                        mBuilder.menulistener.onMenuItemClick((MenuItem) adapter.getItem(position));
                    else if (mBuilder.listener != null)
                        mBuilder.listener.onClick(BottomSheet4.this, ((MenuItem) adapter.getItem(position)).getItemId());
                }
                dismiss();
            }
        });

        setListLayout();
        dialog.setContentView(mDialogView);
        dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                list.setAdapter(adapter);
                list.startLayoutAnimation();
                if (mBuilder.icon == null)
                    icon.setVisibility(View.GONE);
                else {
                    icon.setVisibility(View.VISIBLE);
                    icon.setImageDrawable(mBuilder.icon);
                }
            }
        });
    }


    private void updateSection() {
        actions.removeInvisible();

        if (!mBuilder.grid && actions.size() > 0) {
            int groupId = actions.getItem(0).getGroupId();
            ArrayList<SimpleSectionedGridAdapter.Section> sections = new ArrayList<>();
            for (int i = 0; i < actions.size(); i++) {
                if (actions.getItem(i).getGroupId() != groupId) {
                    groupId = actions.getItem(i).getGroupId();
                    sections.add(new SimpleSectionedGridAdapter.Section(i, null));
                }
            }
            if (sections.size() > 0) {
                SimpleSectionedGridAdapter.Section[] s = new SimpleSectionedGridAdapter.Section[sections.size()];
                sections.toArray(s);
                adapter.setSections(s);
            } else {
                adapter.mSections.clear();
            }
        }
    }

    private void showFullItems() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Transition changeBounds = new ChangeBounds();
            changeBounds.setDuration(300);
            TransitionManager.beginDelayedTransition(list, changeBounds);
        }
        actions = fullMenuItem;
        updateSection();
        adapter.notifyDataSetChanged();
        list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        icon.setVisibility(View.VISIBLE);
        icon.setImageDrawable(close);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShortItems();
            }
        });
        setListLayout();
    }

    private void showShortItems() {
        actions = menuItem;
        updateSection();
        adapter.notifyDataSetChanged();
        setListLayout();

        if (mBuilder.icon == null)
            icon.setVisibility(View.GONE);
        else {
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(mBuilder.icon);
        }
    }

    private boolean hasDivider() {
        return adapter.mSections.size() > 0;
    }

    private void setListLayout() {
        // without divider, the height of gridview is correct
        if (!hasDivider())
            return;
        list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < 16) {
                    //noinspection deprecation
                    list.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                View lastChild = list.getChildAt(list.getChildCount() - 1);
                if (lastChild != null)
                    list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, lastChild.getBottom() + lastChild.getPaddingBottom() + list.getPaddingBottom()));
            }
        });
    }

    public Menu getMenu() {
        return mBuilder.menu;
    }

    /**
     * If you make any changes to menu and try to apply it immediately to your bottomsheet, you should call this.
     */
    public void invalidate() {
        updateSection();
        adapter.notifyDataSetChanged();
        setListLayout();
    }

    @Override
    public void cancel() {

    }


    /**
     * Constructor using a context for this builder and the {@link com.cocosw.bottomsheet.BottomSheet} it creates.
     */
    public static class Builder {

        private final Context context;
        private final ActionMenu menu;

        private CharSequence title;
        private boolean grid;
        private OnClickListener listener;
        private Drawable icon;
        private int limit = -1;
        private MenuItem.OnMenuItemClickListener menulistener;

        private int theme = 0;

        private FragmentManager mFragmentManager;


        public Builder(@NonNull Context context, FragmentManager fragmentManager){

            this.mFragmentManager = fragmentManager;
            this.context = context;
            this.menu = new ActionMenu(context);
        }

//        /**
//         * Show BottomSheet in dark color theme looking
//         *
//         * @return This Builder object to allow for chaining of calls to set methods
//         */
//        public Builder darkTheme() {
//            theme = R.style.BottomSheet_Dialog_Dark;
//            return this;
//        }

        /**
         * Set menu resources as list item to display in BottomSheet
         *
         * @param xmlRes menu resource id
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(@MenuRes int xmlRes) {
            new MenuInflater(context).inflate(xmlRes, menu);
            return this;
        }


        /**
         * Add one item into BottomSheet
         *
         * @param id      ID of item
         * @param iconRes icon resource
         * @param textRes text resource
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @DrawableRes int iconRes, @StringRes int textRes) {
            ActionMenuItem item = new ActionMenuItem(context, 0, id, 0, 0, context.getText(textRes));
            item.setIcon(iconRes);
            menu.add(item);
            return this;
        }

        /**
         * Add one item into BottomSheet
         *
         * @param id   ID of item
         * @param icon icon
         * @param text text
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @NonNull Drawable icon, @NonNull CharSequence text) {
            ActionMenuItem item = new ActionMenuItem(context, 0, id, 0, 0, text);
            item.setIcon(icon);
            menu.add(item);
            return this;
        }

        /**
         * Add one item without icon into BottomSheet
         *
         * @param id      ID of item
         * @param textRes text resource
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @StringRes int textRes) {
            menu.add(0, id, 0, textRes);
            return this;
        }

        /**
         * Add one item without icon into BottomSheet
         *
         * @param id   ID of item
         * @param text text
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @NonNull CharSequence text) {
            menu.add(0, id, 0, text);
            return this;
        }

        /**
         * Set title for BottomSheet
         *
         * @param titleRes title for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder title(@StringRes int titleRes) {
            title = context.getText(titleRes);
            return this;
        }

        /**
         * Remove an item from BottomSheet
         *
         * @param id ID of item
         * @return This Builder object to allow for chaining of calls to set methods
         */
        @Deprecated
        public Builder remove(int id) {
            menu.removeItem(id);
            return this;
        }

        /**
         * Set title for BottomSheet
         *
         * @param icon icon for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder icon(Drawable icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Set title for BottomSheet
         *
         * @param iconRes icon resource id for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder icon(@DrawableRes int iconRes) {
            this.icon = context.getResources().getDrawable(iconRes);
            return this;
        }

        /**
         * Set OnclickListener for BottomSheet
         *
         * @param listener OnclickListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder listener(@NonNull OnClickListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Set OnMenuItemClickListener for BottomSheet
         *
         * @param listener OnMenuItemClickListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder listener(@NonNull MenuItem.OnMenuItemClickListener listener) {
            this.menulistener = listener;
            return this;
        }


        /**
         * Show BottomSheet
         *
         * @return Instance of bottomsheet
         */
        public BottomSheet4 show() {
            dialog = build();
            dialog.show(mFragmentManager, null);
            return dialog;
        }

        /**
         * Show items in grid instead of list
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder grid() {
            this.grid = true;
            return this;
        }

        /**
         * Set initial number of actions which will be shown in current sheet.
         * If more actions need to be shown, a "more" action will be displayed in the last position.
         *
         * @param limitRes resource id for initial number of actions
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder limit(@IntegerRes int limitRes) {
            limit = context.getResources().getInteger(limitRes);
            return this;
        }


        private BottomSheet4 dialog;

        /**
         * Create a BottomSheet but not show it
         *
         * @return Instance of bottomsheet
         */
        @SuppressLint("Override")
        public BottomSheet4 build() {
            BottomSheet4 dialog = new BottomSheet4();
            dialog.add(this);
            return dialog;
        }

        /**
         * Set title for BottomSheet
         *
         * @param title title for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder title(CharSequence title) {
            this.title = title;
            return this;
        }
    }
}

