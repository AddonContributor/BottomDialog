package com.brmnt.bottomdialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.IdRes;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;



/**
 * @author Bramengton on 22/08/2017.
 */
class ActionsAdapter extends BaseAdapter {
    private final SparseIntArray hidden = new SparseIntArray();
    private boolean mCollapseListIcons;
    private LayoutInflater mLayoutInflater;
    private ActionMenu mActionMenu;
    private int mResLayout;

    ActionsAdapter(final Dialog dialog, ActionMenu actions, @IdRes int itemLayoutId, boolean collapseListIcons){
        mLayoutInflater = dialog.getLayoutInflater();
        mResLayout = itemLayoutId;
        mActionMenu = actions;
        mCollapseListIcons = collapseListIcons;
    }

    ActionsAdapter(final Context context, ActionMenu actions, @IdRes int itemLayoutId, boolean collapseListIcons){
        mLayoutInflater = LayoutInflater.from(context);
        mResLayout = itemLayoutId;
        mActionMenu = actions;
        mCollapseListIcons = collapseListIcons;
    }

    @Override
    public int getCount() {
        return mActionMenu.size() - hidden.size();
    }

    @Override
    public MenuItem getItem(int position) {
        return mActionMenu.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).isEnabled();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(mResLayout, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.list_title);
            holder.image = (ImageView) convertView.findViewById(R.id.list_image);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //Нахер надо - не ясно :-)))
        for (int i = 0; i < hidden.size(); i++) {
            if (hidden.valueAt(i) <= position)
                position++;
        }

        MenuItem item = getItem(position);

        holder.title.setText(item.getTitle());
        if (item.getIcon() == null)
            holder.image.setVisibility(mCollapseListIcons ? View.GONE : View.INVISIBLE);
        else {
            holder.image.setVisibility(View.VISIBLE);
            holder.image.setImageDrawable(item.getIcon());
        }

        holder.image.setEnabled(item.isEnabled());
        holder.title.setEnabled(item.isEnabled());

        return convertView;
    }

    private class ViewHolder {
        private TextView title;
        private ImageView image;
    }
}
