package androiddegree.udacity.ememobong.rxageracalculator;

import android.widget.SeekBar;

import com.google.android.agera.MutableRepository;

/**
 * Created by Bless on 9/8/2017.
 */

public class RepositorySeekBarListener implements SeekBar.OnSeekBarChangeListener {

    private MutableRepository<Integer> mRepository;

    public RepositorySeekBarListener(MutableRepository<Integer> repository) {
        mRepository = repository;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean userInitiated) {
        mRepository.accept(i);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
