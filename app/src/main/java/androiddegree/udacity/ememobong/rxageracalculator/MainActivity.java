package androiddegree.udacity.ememobong.rxageracalculator;

import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.agera.MutableRepository;
import com.google.android.agera.Repositories;
import com.google.android.agera.Repository;
import com.google.android.agera.RepositoryConfig;
import com.google.android.agera.Result;
import com.google.android.agera.Updatable;

public class MainActivity extends AppCompatActivity {

    private static final String VALUE_2 = "value2";
    private static final String VALUE_1 = "value1";
    private static final String OPERATION_KEY = "operation_key";
    public static final String ANIMATIONS_ENABLED_KEY = "animations_enabled_key";

    //    Repository for storing the first value to be calculated
    private MutableRepository<Integer> mValue1Repo = Repositories.mutableRepository(0);
    //    Repository for storing the second value to be calculated
    private MutableRepository<Integer> mValue2Repo = Repositories.mutableRepository(0);
    //    Repository for storing the result to be calculated
    private Repository<Result<String>> mResultRepository;
    // Repository for storing the operation to be performed
    private MutableRepository<Result<Integer>> mOperationSelector =
            Repositories.mutableRepository(Result.absent());


    // Updateables that will be called when the repository values changes ie when an event has occured
    private Updatable mValue1TVupdatable;
    private Updatable mValue2TVupdatable;
    private Updatable mResultUpdatable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState !=  null && savedInstanceState.containsKey(VALUE_1)){
            mValue1Repo.accept(savedInstanceState.getInt(VALUE_1));
            mValue2Repo.accept(savedInstanceState.getInt(VALUE_2));
            mOperationSelector.accept(Result.present(savedInstanceState.getInt(OPERATION_KEY)));
        }

    }

    @Override
    protected void onStart() {
        super.onStart();




        // set the event sources to work with their repository
        ((SeekBar) findViewById(R.id.seekBar1)).setOnSeekBarChangeListener(
                new RepositorySeekBarListener(mValue1Repo));

        ((SeekBar) findViewById(R.id.seekBar2)).setOnSeekBarChangeListener(
                new RepositorySeekBarListener(mValue2Repo));

        //the result repository is a complex repository so taking so extra care to make sure it works well
        mResultRepository = Repositories.repositoryWithInitialValue(Result.<String>absent())
                .observe(mValue1Repo, mValue2Repo, mOperationSelector) // observe for when the repository changes
                .onUpdatesPerLoop()  // updates after all tasks in the queue has been executed
                .goTo(CalculatorExecutor.EXECUTOR) // go to Executor for managing multiple threads
                .attemptTransform(CalculatorOperations::keepCpuBusy).orEnd(Result::failure) //data processing flow provides                                                                                                   failure-aware directives that allow                                                                                               terminating the flow in case of failure
                .getFrom(mValue1Repo)    // obtain values for processing
                .mergeIn(mValue2Repo, Pair::create)  // merge value from first and second repo and create a pair
                .attemptMergeIn(mOperationSelector, CalculatorOperations::attemptOperation)  // try to perform calculator                                                                                                         operation of add, subtract,                                                                                                       multiply or divide
                .orEnd(Result::failure)
                .thenTransform(input -> Result.present(input.toString())) // transform calculated result to be presented
                .onConcurrentUpdate(RepositoryConfig.SEND_INTERRUPT)
                .compile();

        // initialise the Updater to update the textviews from values from the repository always
        mValue1TVupdatable = () -> ((TextView) findViewById(R.id.value1)).setText(
                mValue1Repo.get().toString());

        mValue2TVupdatable = () -> ((TextView) findViewById(R.id.value2)).setText(
                mValue2Repo.get().toString());

        // initialise the Updater for the resulttextview to check for errors and set textview correctly
        TextView resultTextView = (TextView) findViewById(R.id.textViewResult);
        mResultUpdatable = () -> mResultRepository
                .get()
                .ifFailedSendTo(t -> Toast.makeText(this, t.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show())
                .ifFailedSendTo(t -> {
                    if (t instanceof ArithmeticException) {
                        resultTextView.setText("DIV#0");
                    } else {
                        resultTextView.setText("N/A");
                    }
                })
                .ifSucceededSendTo(resultTextView::setText);

        setUpdatables();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mOperationSelector.get().isPresent()) {
            outState.putInt(VALUE_1, mValue1Repo.get());
            outState.putInt(VALUE_2, mValue2Repo.get());
            outState.putInt(OPERATION_KEY, mOperationSelector.get().get());
        }
        super.onSaveInstanceState(outState);
    }

    private void setUpdatables() {
        //add updateables to the repository
        mValue1Repo.addUpdatable(mValue1TVupdatable);
        mValue2Repo.addUpdatable(mValue2TVupdatable);
        mResultRepository.addUpdatable(mResultUpdatable);

        // call update on the when the event repository is observing for changes
        mValue1TVupdatable.update();
        mValue2TVupdatable.update();
        mResultUpdatable.update();
    }

    @Override
    protected void onStop() {
        removeUpdatables();
        super.onStop();
    }

    private void removeUpdatables() {
        mValue1Repo.removeUpdatable(mValue1TVupdatable);
        mValue2Repo.removeUpdatable(mValue2TVupdatable);
        mResultRepository.removeUpdatable(mResultUpdatable);
    }

    // set the radio button to notify the updater when changed
    public void onRadioButtonClicked(View view) {
        mOperationSelector.accept(Result.present(view.getId()));
    }

}
