package com.cocosw.bottomsheet.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

/**
 * Project: gradle
 * Created by LiaoKai(soarcn) on 2014/9/22.
 */
public class ListActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        if (getIntent().getBooleanExtra("style", false)) {
//            setTheme(R.style.StyleTheme);
//        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_fragment);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_content, new FragmentList()).commit();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }
}
