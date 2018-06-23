package reeiss.bonree.ble_test.smarthardware;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import reeiss.bonree.ble_test.utils.FragmentFactory;
import reeiss.bonree.ble_test.R;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView mBottomNavigationView;
    private Fragment[] mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainbaidu);
        initView();
    }

    private void initView() {
        mFragments = new Fragment[4];
        mBottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                onTabItemSelected(item.getItemId());
                return true;
            }
        });

        onTabItemSelected(R.id.tab_menu_home);
    }

    private void onTabItemSelected(int id) {
        Fragment fragment = null;
        switch (id) {
            case R.id.tab_menu_home:
                fragment = mFragments[0];
                FragmentFactory.getInstance().changeFragment(0, R.id.fragment, getSupportFragmentManager());
                break;
            case R.id.tab_menu_discovery:
                fragment = mFragments[1];
                FragmentFactory.getInstance().changeFragment(1, R.id.fragment, getSupportFragmentManager());
                break;
            case R.id.tab_menu_attention:
                fragment = mFragments[2];
                FragmentFactory.getInstance().changeFragment(2, R.id.fragment, getSupportFragmentManager());
                break;
            case R.id.tab_menu_profile:
                fragment = mFragments[3];
                FragmentFactory.getInstance().changeFragment(3, R.id.fragment, getSupportFragmentManager());
                break;
        }
     /*   if (fragment != null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (currentFragment != null) {
                fragmentTransaction.hide(currentFragment);
            }
            fragmentTransaction.replace(R.id.fragment, fragment).commit();
            currentFragment = fragment;
        }*/
    }

   /* @Override
    public void onClick(View view) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (view.getId()) {
            case R.id.image1:
                if (firstFragment == null)
                    firstFragment = new FirstFragment();
                transaction.replace(R.id.fragment, firstFragment);
                transaction.commit();
                break;
            case R.id.image2:
                if (secondFragment == null)
                    secondFragment = new SecondFragment();
                transaction.replace(R.id.fragment, secondFragment);
                transaction.commit();
                break;
            case R.id.image3:
                if (thirdFragment == null)
                    thirdFragment = new ThreeFragment();
                transaction.replace(R.id.fragment, thirdFragment);
                transaction.commit();
                break;
            case R.id.image4:
                if (fourthFragment == null)
                    fourthFragment = new FourFragment();
                transaction.replace(R.id.fragment, fourthFragment);
                transaction.commit();
                break;
            default:
                break;
        }
    }*/
}