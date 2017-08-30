package com.cocosw.bottomsheet.example;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.brmnt.bottomdialog.BottomDialog;
import com.brmnt.bottomdialog.BottomDialogHelper;
import com.cocosw.query.CocoQuery;

/**
 * @author by Bramengton
 * @date 30.08.17.
 */
public class FragmentList extends Fragment implements AdapterView.OnItemClickListener {

    private CocoQuery q;
    private int action;
    private ArrayAdapter<String> adapter;
    private BottomDialog.Builder sheet;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        action = getActivity().getIntent().getFlags();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        q = new CocoQuery(getActivity());
        getActivity().setTitle(getActivity().getIntent().getStringExtra("title"));
        String[] items = new String[]{"Janet Perkins", "Mary Johnson", "Peter Carlsson", "Trevor Hansen", "Aaron Bennett"};
        q.id(R.id.listView)
                .adapter(adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, items))
                .itemClicked(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.ui_main, container, false);
    }


    @SuppressWarnings("deprecation")
    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        doDialog(position).show();
    }

    private Drawable getRoundedBitmap(int imageId) {
        Bitmap src = BitmapFactory.decodeResource(getResources(), imageId);
        Bitmap dst;
        if (src.getWidth() >= src.getHeight()) {
            dst = Bitmap.createBitmap(src, src.getWidth() / 2 - src.getHeight() / 2, 0, src.getHeight(), src.getHeight());
        } else {
            dst = Bitmap.createBitmap(src, 0, src.getHeight() / 2 - src.getWidth() / 2, src.getWidth(), src.getWidth());
        }
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), dst);
        roundedBitmapDrawable.setCornerRadius(dst.getWidth() / 2);
        roundedBitmapDrawable.setAntiAlias(true);
        return roundedBitmapDrawable;
    }

    private BottomDialog.Builder getShareActions(String text) {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);

        return BottomDialogHelper.shareAction(getActivity(), this.getFragmentManager(), shareIntent);
    }


    private void setClick(String name, int which) {
        switch (which) {
            case R.id.share:
                q.toast("Share to " + name);
                break;
            case R.id.upload:
                q.toast("Upload for " + name);
                break;
            case R.id.call:
                q.toast("Call to " + name);
                break;
            case R.id.help:
                q.toast("Help me!");
                break;
        }
    }


    private BottomDialog.Builder doDialog(final int position){
        switch (action) {
            case 0:
                sheet = new BottomDialog.Builder(getContext(), this.getFragmentManager())
                        .icon(getRoundedBitmap(R.drawable.icon))
                        .title("To " + adapter.getItem(position))
                        .sheet(R.menu.list)
                        .setOnClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setClick(adapter.getItem(position), which);
                            }
                        });
                sheet.build();
                break;
            case 1:
                sheet = new BottomDialog.Builder(getContext(), this.getFragmentManager())
                        .sheet(R.menu.noicon)
                        .setOnClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setClick(adapter.getItem(position), which);
                            }
                        });
                sheet.build();
                break;
            case 2:
                sheet = new BottomDialog.Builder(getContext(), this.getFragmentManager())
                        .darkTheme()
                        .title("To " + adapter.getItem(position))
                        .sheet(R.menu.list)
                        .setOnClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setClick(adapter.getItem(position), which);
                            }
                        });
                sheet.build();
                break;
            case 3:
                sheet = new BottomDialog.Builder(getContext(), this.getFragmentManager())
                        .sheet(R.menu.list)
                        .setOnClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setClick(adapter.getItem(position), which);
                            }
                        });
                sheet.grid().build();
                break;
            case 4:
                sheet = new BottomDialog.Builder(getContext(), this.getFragmentManager())
                        .setTheme(R.style.BottomDialog_CustomizedDialog)
                        .title("To " + adapter.getItem(position))
                        .sheet(R.menu.list)
                        .setOnClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setClick(adapter.getItem(position), which);
                            }
                        });
                sheet.build();
                break;
            case 5:
                sheet = new BottomDialog.Builder(getContext(), this.getFragmentManager())
                        .title("To " + adapter.getItem(position))
                        .sheet(R.menu.longlist)
                        .setOnClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setClick(adapter.getItem(position), which);
                            }
                        });
                sheet
                        .limit(R.integer.bd_initial_list_row)
                        .build();
                break;
            case 6:
                sheet = getShareActions("Hello " + adapter.getItem(position));
                sheet.title("Share To " + adapter.getItem(position))
                        .limit(R.integer.no_limit)
                        .build();
                break;
            case 7:
                sheet = getShareActions("Hello " + adapter.getItem(position));
                sheet.title("Share To " + adapter.getItem(position)).build();
                break;
            case 8:
                sheet = new BottomDialog.Builder(getContext(), this.getFragmentManager());
                sheet.icon(getRoundedBitmap(R.drawable.icon))
                        .title("To " + adapter.getItem(position))
                        .sheet(R.menu.list)
                        .setOnClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setClick(adapter.getItem(position), which);
                            }
                        }).build();
                final Menu menu = sheet.getMenu();
                menu.getItem(0).setTitle("MenuClickListener");
                menu.getItem(0).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        q.alert("OnMenuItemClickListener", "You can set OnMenuItemClickListener for each item");
                        return true;
                    }
                });
                menu.getItem(1).setVisible(false);
                menu.getItem(2).setEnabled(false);
                menu.add(Menu.NONE, 23, Menu.NONE, "Fresh meal!");
                menu.findItem(23).setIcon(R.drawable.perm_group_user_dictionary);
                menu.findItem(23).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        q.toast("Hello");
                        return true;
                    }
                });
                menu.setGroupVisible(android.R.id.empty,false);
                break;
            case 9:
                sheet = new BottomDialog.Builder(getContext(), this.getFragmentManager())
                        .setTheme(R.style.BottomDialog_CustomizedDialog)
                        .grid()
                        .title("To " + adapter.getItem(position))
                        .sheet(R.menu.list)
                        .setOnClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setClick(adapter.getItem(position), which);
                            }
                        });
                sheet.build();

                sheet.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        q.toast("I'm showing");
                    }
                });

//                sheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        q.toast("I'm dismissing");
//                    }
//                });
                break;

        }

        return sheet;
    }
}
