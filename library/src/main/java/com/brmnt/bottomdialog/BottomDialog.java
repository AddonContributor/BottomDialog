package com.brmnt.bottomdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.*;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * @author Bramengton on 21/08/2017.
 */
public class BottomDialog extends FragmentBottomDialog<BottomDialog.Builder> {
    private Builder mBuilder;

    @Override
    void setDialogStyledAttributes(Context context, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.BottomDialog, R.attr.bottomDialogStyle, defStyle);
        try {
            more = a.getDrawable(R.styleable.BottomDialog_bd_MoreDrawable);
            close = a.getDrawable(R.styleable.BottomDialog_bd_CloseDrawable);
            moreText = a.getString(R.styleable.BottomDialog_bd_MoreText);
            collapseListIcons = a.getBoolean(R.styleable.BottomDialog_bd_CollapseListIcons, true);
            mHeaderLayoutId = a.getResourceId(R.styleable.BottomDialog_bd_HeaderLayout, R.layout.dialog_header);
            mListItemLayoutId = a.getResourceId(R.styleable.BottomDialog_bd_ListItemLayout, R.layout.list_entry);
            mGridItemLayoutId = a.getResourceId(R.styleable.BottomDialog_bd_GridItemLayout, R.layout.grid_entry);
        } finally {
            a.recycle();
        }
    }

    @Override
    int setDialogStyle() {
        return getBuilder().getTheme();
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        mBuilder = getBuilder();
        init(dialog);
    }

    private String moreText;
    private Drawable close;
    private Drawable more;
    private int mHeaderLayoutId;
    private int mListItemLayoutId;
    private int mGridItemLayoutId;

    private boolean collapseListIcons;
    private PinnedSectionGridView mGridView;
    private ActionsAdapter mBaseAdapter;
    private SimpleSectionedGridAdapter mGridAdapter;

    private ImageView mIcon;

    private ActionMenu fullMenuItem;
    private ActionMenu menuItem;
    private ActionMenu mActionMenu;


    int limit = -1;
    /**
     * Hacky way to get gridview's column number
     */
    private int getNumColumns() {
        try {
            Field numColumns = GridView.class.getDeclaredField("mRequestedNumColumns");
            numColumns.setAccessible(true);
            return numColumns.getInt(mGridView);
        } catch (Exception e) {
            return 1;
        }
    }

    private void init(final Dialog dialog) {
        final View mDialogView = dialog.getLayoutInflater().inflate(R.layout.bottom_sheet_dialog, null);
        final LinearLayout header = (LinearLayout) dialog.getLayoutInflater().inflate(mHeaderLayoutId, null);
        final LinearLayout mainLayout = (LinearLayout) mDialogView.findViewById(R.id.bs_main);

        mainLayout.addView(header, 0);

        final TextView title = (TextView) mDialogView.findViewById(R.id.bottom_dialog_title);
        if (mBuilder.getTitle() != null) {
            title.setVisibility(View.VISIBLE);
            title.setText(mBuilder.getTitle());
        }

        mIcon = (ImageView) mDialogView.findViewById(R.id.bottom_dialog_title_image);
        mGridView = (PinnedSectionGridView) mDialogView.findViewById(R.id.bottom_sheet_gridview);
        if (!mBuilder.isGrid()) {
            mGridView.setNumColumns(1);
        }

        if (mBuilder.isGrid()) {
            for (int i = 0; i < getMenu().size(); i++) {
                if (getMenu().getItem(i).getIcon() == null)
                    throw new IllegalArgumentException("You must set mIcon for each items in mGrid style");
            }
        }


        if (mBuilder.getLimit() > 0)
            limit = mBuilder.getLimit() * getNumColumns();
        else
            limit = Integer.MAX_VALUE;

        menuItem = mActionMenu = mBuilder.mActionMenu;
        // over the initial numbers
        if (getMenu().size() > limit) {
            fullMenuItem = mBuilder.mActionMenu;
            menuItem = mBuilder.mActionMenu.clone(limit-1);
            ActionMenuItem item = new ActionMenuItem(getContext(), 0, R.id.bottom_dialog_button_more, 0, limit - 1, moreText);
            item.setIcon(more);
            menuItem.add(item);
            mActionMenu = menuItem;
        }

        final int layout = mBuilder.isGrid() ? mGridItemLayoutId : mListItemLayoutId;
        mBaseAdapter = new ActionsAdapter(dialog, layout, collapseListIcons);
        mBaseAdapter.updateList(mActionMenu);
        mGridAdapter = new SimpleSectionedGridAdapter(dialog, mBaseAdapter, R.layout.list_divider, R.id.header_layout, R.id.header);
        mGridAdapter.setGridView(mGridView);


        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (((MenuItem) mGridAdapter.getItem(position)).getItemId() == R.id.bottom_dialog_button_more) {
                    showFullItems();
                    return;
                }

                if (!((ActionMenuItem) mGridAdapter.getItem(position)).invoke()) {
                    if (mBuilder.mMenuListener != null)
                        mBuilder.mMenuListener.onMenuItemClick((MenuItem) mGridAdapter.getItem(position));
                    else if (mBuilder.clickListener != null)
                        mBuilder.clickListener.onClick(dialog, ((MenuItem) mGridAdapter.getItem(position)).getItemId());
                }
                dialog.dismiss();
            }
        });

        dialog.setContentView(mDialogView);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (mBuilder.showListener != null)
                    mBuilder.showListener.onShow(dialogInterface);
                mGridView.setAdapter(mGridAdapter);
                mGridView.startLayoutAnimation();
                if (mBuilder.getIcon() == null)
                    mIcon.setVisibility(View.GONE);
                else {
                    mIcon.setVisibility(View.VISIBLE);
                    mIcon.setImageDrawable(mBuilder.getIcon());
                }
            }
        });

//        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//                if (mBuilder.dismissListener != null) mBuilder.dismissListener.onDismiss(dialog);
//            }
//        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mBuilder.cancelListener != null) mBuilder.cancelListener.onCancel(dialog);
            }
        });
    }


    private void updateSection() {
        mActionMenu.removeInvisible();

        if (!mBuilder.isGrid() && mActionMenu.size() > 0) {
            int groupId = mActionMenu.getItem(0).getGroupId();
            ArrayList<SimpleSectionedGridAdapter.Section> sections = new ArrayList<>();
            for (int i = 0; i < mActionMenu.size(); i++) {
                if (mActionMenu.getItem(i).getGroupId() != groupId) {
                    groupId = mActionMenu.getItem(i).getGroupId();
                    sections.add(new SimpleSectionedGridAdapter.Section(i, null));
                }
            }
            if (sections.size() > 0) {
                SimpleSectionedGridAdapter.Section[] s = new SimpleSectionedGridAdapter.Section[sections.size()];
                sections.toArray(s);
                mGridAdapter.setSections(s);
            } else {
                mGridAdapter.mSections.clear();
            }
        }
    }

    private void showFullItems() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Transition changeBounds = new ChangeBounds();
            changeBounds.setDuration(300);
            TransitionManager.beginDelayedTransition(mGridView, changeBounds);
        }
        mActionMenu = fullMenuItem;
        updateSection();
        mBaseAdapter.updateList(mActionMenu);
        mGridAdapter.notifyDataSetChanged();
        mGridView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));
        mIcon.setVisibility(View.VISIBLE);
        mIcon.setImageDrawable(close);
        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShortItems();
            }
        });
    }

    private void showShortItems() {
        mActionMenu = menuItem;
        updateSection();
        mBaseAdapter.updateList(mActionMenu);
        mGridAdapter.notifyDataSetChanged();

        if (mBuilder.getIcon() == null)
            mIcon.setVisibility(View.GONE);
        else {
            mIcon.setVisibility(View.VISIBLE);
            mIcon.setImageDrawable(mBuilder.getIcon());
        }
    }

    public Menu getMenu() {
        return mBuilder.mActionMenu;
    }

    /**
     * If you make any changes to mActionMenu and try to apply it immediately to your bottomsheet, you should call this.
     */
    @SuppressWarnings("unused")
    public void invalidate() {
        updateSection();
        mGridAdapter.notifyDataSetChanged();
    }

    /**
     * Constructor using a mContext for this builder and the {@link BottomDialog} it creates.
     */
    public static class Builder extends BuilderBottomDialog<BottomDialog>{

        private Context mContext;
        private ActionMenu mActionMenu;

        private MenuItem.OnMenuItemClickListener mMenuListener;
        private DialogInterface.OnDismissListener dismissListener;
        private DialogInterface.OnCancelListener cancelListener;
        private DialogInterface.OnShowListener showListener;
        private DialogInterface.OnClickListener clickListener;

        private BottomDialog dialog;

        public Builder(Context context, FragmentManager manager) {
            super(manager);
            dialog = new BottomDialog();
            this.mContext = context;
            this.mActionMenu = new ActionMenu(context);
        }

        public Builder(@NonNull Context context, FragmentManager manager, @StyleRes int theme){
            super(manager);
            dialog = new BottomDialog();
            dialog.setStyle(DialogFragment.STYLE_NO_TITLE, theme);
            this.mContext = context;
            this.mActionMenu = new ActionMenu(context);
        }

        @Override
        public BottomDialog setDialog() {
            return new BottomDialog();
        }

        public ActionMenu getMenu(){
            return this.mActionMenu;
        }

        /**
         * Show BottomSheet in dark color theme looking
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder darkTheme() {
            Theme(R.style.BottomDialog_Dialog_Dark);
            return this;
        }

        public Builder setTheme(@StyleRes int style) {
            Theme(style);
            return this;
        }

        public Builder grid(){
            setGrid();
            return this;
        }

        /**
         * Set mActionMenu resources as list item to display in BottomSheet
         *
         * @param xmlRes mActionMenu resource id
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(@MenuRes int xmlRes) {
            new MenuInflater(mContext).inflate(xmlRes, mActionMenu);
            return this;
        }

        /**
         * Add one item into BottomSheet
         *
         * @param id      ID of item
         * @param iconRes mIcon resource
         * @param textRes text resource
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @DrawableRes int iconRes, @StringRes int textRes) {
            ActionMenuItem item = new ActionMenuItem(mContext, 0, id, 0, 0, mContext.getText(textRes));
            item.setIcon(iconRes);
            mActionMenu.add(item);
            return this;
        }

        /**
         * Add one item into BottomSheet
         *
         * @param id   ID of item
         * @param icon mIcon
         * @param text text
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @NonNull Drawable icon, @NonNull CharSequence text) {
            ActionMenuItem item = new ActionMenuItem(mContext, 0, id, 0, 0, text);
            item.setIcon(icon);
            mActionMenu.add(item);
            return this;
        }

        /**
         * Add one item without mIcon into BottomSheet
         *
         * @param id      ID of item
         * @param textRes text resource
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @StringRes int textRes) {
            mActionMenu.add(0, id, 0, textRes);
            return this;
        }

        /**
         * Add one item without mIcon into BottomSheet
         *
         * @param id   ID of item
         * @param text text
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @NonNull CharSequence text) {
            mActionMenu.add(0, id, 0, text);
            return this;
        }

        /**
         * Set mTitle for BottomSheet
         *
         * @param titleRes mTitle for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder title(@StringRes int titleRes) {
            setTitle(titleRes);
            return this;
        }

        public Builder title(String titleRes) {
            setTitle(titleRes);
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
            mActionMenu.removeItem(id);
            return this;
        }

        /**
         * Set mTitle for BottomSheet
         *
         * @param icon mIcon for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder icon(Drawable icon) {
            setIcon(icon);
            return this;
        }

        /**
         * Set mTitle for BottomSheet
         *
         * @param iconRes mIcon resource id for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder icon(@DrawableRes int iconRes) {
            setIcon(iconRes);
            return this;
        }


        /**
         * Set OnMenuItemClickListener for BottomSheet
         *
         * @param listener OnMenuItemClickListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnClickListener(@NonNull MenuItem.OnMenuItemClickListener listener) {
            this.mMenuListener = listener;
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
            setLimit(limitRes);
            return this;
        }

        /**
         * Set click listener for BottomSheet
         *
         * @param listener DialogInterface.OnClickListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnClickListener(@NonNull DialogInterface.OnClickListener listener) {
            this.clickListener = listener;
            return this;
        }

        /**
         * Set show listener for BottomSheet
         *
         * @param listener DialogInterface.OnShowListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnShowListener(@NonNull DialogInterface.OnShowListener listener) {
            this.showListener = listener;
            return this;
        }

        /**
         * Set cancel listener for BottomSheet
         *
         * @param listener DialogInterface.OnCancelListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnCancelListener(@NonNull DialogInterface.OnCancelListener listener) {
            this.cancelListener = listener;
            return this;
        }


//        /**
//         * Set dismiss listener for BottomSheet
//         *
//         * @param listener DialogInterface.OnDismissListener for BottomSheet
//         * @return This Builder object to allow for chaining of calls to set methods
//         */
//        public Builder setOnDismissListener(@NonNull DialogInterface.OnDismissListener listener) {
//            this.dismissListener = listener;
//            return this;
//        }
    }
}

