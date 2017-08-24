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
import android.view.*;
import android.widget.*;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * @author Bramengton on 21/08/2017.
 */
public class BottomDialog extends FragmentBottomDialog<BottomDialog.Builder> {
    private Builder mBuilder;

    @Override
    void setDialogStyledAttributes(Context context) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.BottomDialog, R.attr.bottomDialogStyle, 0);
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
        return getBuilder().mTheme;
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
    private PinnedSectionGridView list;
    private SimpleSectionedGridAdapter adapter;

    private ImageView icon;

    private int limit = -1;

    private ActionMenu fullMenuItem;
    private ActionMenu menuItem;
    private ActionMenu actions;

    private DialogInterface.OnDismissListener dismissListener;
    private DialogInterface.OnCancelListener cancelListener;
    private DialogInterface.OnShowListener showListener;
    private DialogInterface.OnClickListener clickListener;

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

        final TextView title = (TextView) mDialogView.findViewById(R.id.bottom_dialog_title);
        if (mBuilder.mTitle != null) {
            title.setVisibility(View.VISIBLE);
            title.setText(mBuilder.mTitle);
        }

        icon = (ImageView) mDialogView.findViewById(R.id.bottom_dialog_title_image);
        list = (PinnedSectionGridView) mDialogView.findViewById(R.id.bottom_sheet_gridview);
        if (!mBuilder.mGrid) {
            list.setNumColumns(1);
        }

        if (mBuilder.mGrid) {
            for (int i = 0; i < getMenu().size(); i++) {
                if (getMenu().getItem(i).getIcon() == null)
                    throw new IllegalArgumentException("You must set mIcon for each items in mGrid style");
            }
        }

        if (mBuilder.limit > 0)
            limit = mBuilder.limit * getNumColumns();
        else
            limit = Integer.MAX_VALUE;


        actions = mBuilder.mActionMenu;
        menuItem = actions;
        // over the initial numbers
        if (getMenu().size() > limit) {
            fullMenuItem = mBuilder.mActionMenu;
            menuItem = mBuilder.mActionMenu.clone(limit - 1);
            ActionMenuItem item = new ActionMenuItem(getContext(), 0, R.id.bottom_dialog_button_more, 0, limit - 1, moreText);
            item.setIcon(more);
            menuItem.add(item);
            actions = menuItem;
        }

        final int layout = mBuilder.mGrid ? mGridItemLayoutId : mListItemLayoutId;
        final ActionsAdapter baseAdapter = new ActionsAdapter(dialog, actions, layout, collapseListIcons);
        adapter = new SimpleSectionedGridAdapter(dialog, baseAdapter, R.layout.list_divider, R.id.header_layout, R.id.header);
        adapter.setGridView(list);


        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (((MenuItem) adapter.getItem(position)).getItemId() == R.id.bottom_dialog_button_more) {
                    showFullItems();
                    return;
                }

                if (!((ActionMenuItem) adapter.getItem(position)).invoke()) {
                    if (mBuilder.mMenuListener != null)
                        mBuilder.mMenuListener.onMenuItemClick((MenuItem) adapter.getItem(position));
                    else if (clickListener != null)
                        clickListener.onClick(dialog, ((MenuItem) adapter.getItem(position)).getItemId());
                }
                dialog.dismiss();
            }
        });

        setListLayout();
        dialog.setContentView(mDialogView);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (showListener != null)
                    showListener.onShow(dialogInterface);
                list.setAdapter(adapter);
                list.startLayoutAnimation();
                if (mBuilder.mIcon == null)
                    icon.setVisibility(View.GONE);
                else {
                    icon.setVisibility(View.VISIBLE);
                    icon.setImageDrawable(mBuilder.mIcon);
                }
            }
        });

//        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//
//                Activity activity = getActivity();
//                if(activity instanceof DialogInterface.OnCancelListener)
//                    ((DialogInterface.OnCancelListener)activity).onCancel(dialog);
//
//
//            }
//        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (cancelListener != null)
                    cancelListener.onCancel(dialog);
            }
        });
    }


    private void updateSection() {
        actions.removeInvisible();

        if (!mBuilder.mGrid && actions.size() > 0) {
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

        if (mBuilder.mIcon == null)
            icon.setVisibility(View.GONE);
        else {
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(mBuilder.mIcon);
        }
    }

    @SuppressWarnings("deprecation")
    private void setListLayout() {
        // without divider, the height of gridview is correct
        if (adapter.mSections.size() <= 0)
            return;
        list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //noinspection deprecation
                if (Build.VERSION.SDK_INT < 16) list.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                else list.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                View lastChild = list.getChildAt(list.getChildCount() - 1);
                if (lastChild != null)
                    list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, lastChild.getBottom() + lastChild.getPaddingBottom() + list.getPaddingBottom()));
            }
        });
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
        adapter.notifyDataSetChanged();
        setListLayout();
    }

    /**
     * Constructor using a mContext for this builder and the {@link BottomDialog} it creates.
     */
    public static class Builder extends BuilderBottomDialog<BottomDialog>{

        private final Context mContext;
        private final ActionMenu mActionMenu;

        private CharSequence mTitle;
        private boolean mGrid;

        private Drawable mIcon;
        private int limit = -1;
        private MenuItem.OnMenuItemClickListener mMenuListener;


        private int mTheme = 0;

        private BottomDialog dialog;

        public Builder(Context context, FragmentManager manager) {
            super(context, manager);
            dialog = new BottomDialog();
            this.mContext = context;
            this.mActionMenu = new ActionMenu(context);
        }

        public Builder(@NonNull Context context, FragmentManager manager, @StyleRes int theme){
            super(context, manager);
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
            setTheme(R.style.BottomDialog_Dialog_Dark);
            return this;
        }

        /**
         * Show BottomSheet in dark color theme looking
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTheme(@StyleRes int style) {
            mTheme= style;
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
            mTitle = mContext.getText(titleRes);
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
            this.mIcon = icon;
            return this;
        }

        /**
         * Set mTitle for BottomSheet
         *
         * @param iconRes mIcon resource id for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder icon(@DrawableRes int iconRes) {
            this.mIcon = mContext.getResources().getDrawable(iconRes);
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
         * Show items in mGrid instead of list
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder grid() {
            this.mGrid = true;
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
            limit = mContext.getResources().getInteger(limitRes);
            return this;
        }

        /**
         * Set mTitle for BottomSheet
         *
         * @param title mTitle for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder title(CharSequence title) {
            this.mTitle = title;
            return this;
        }

        /**
         * Set click listener for BottomSheet
         *
         * @param listener DialogInterface.OnClickListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnClickListener(@NonNull DialogInterface.OnClickListener listener) {
            dialog.clickListener = listener;
            return this;
        }

        /**
         * Set show listener for BottomSheet
         *
         * @param listener DialogInterface.OnShowListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnShowListener(@NonNull DialogInterface.OnShowListener listener) {
            dialog.showListener = listener;
            return this;
        }

        /**
         * Set cancel listener for BottomSheet
         *
         * @param listener DialogInterface.OnCancelListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnCancelListener(@NonNull DialogInterface.OnCancelListener listener) {
            dialog.cancelListener = listener;
            return this;
        }


//        /**
//         * Set dismiss listener for BottomSheet
//         *
//         * @param listener DialogInterface.OnDismissListener for BottomSheet
//         * @return This Builder object to allow for chaining of calls to set methods
//         */
//        public Builder setOnDismissListener(@NonNull DialogInterface.OnDismissListener listener) {
//            dialog.dismissListener = listener;
//            return this;
//        }
    }
}
