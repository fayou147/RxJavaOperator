package com.fy.operator.item;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.fy.operator.LogUtils;
import com.fy.operator.R;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class MergeOperatorActivity extends AppCompatActivity {

    @BindView(R.id.et_phone)
    EditText etPhone;
    @BindView(R.id.et_pwd)
    EditText etPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_operator);
        ButterKnife.bind(this);
        zip();
    }

    /**
     * 返回一个Flowable，它发出一个指定组合函数的结果，该function应用于两个publisher按顺序的发射item的组合
     * <b>NOTE:</b>
     * 将多个Observable发射的数据按顺序组合起来，每个数据只能组合一次，而且都是有序的。
     * 最终组合的数据的数量由发射数据最少的Observable来决定。
     * <p>
     * zip与merge,concat的区别。merge,concat类似相加的效果，发射多少个item是a+b的效果
     * zip类似合并的意思，a+b= ab的效果
     */
    private void zip() {
        Flowable<Long> a = Flowable.intervalRange(0, 10, 0, 1000, TimeUnit.MILLISECONDS);
        Flowable<String> b = Flowable.just("a", "b", "c", "d", "e");

        /**
         * value:0a
         * value:1b
         * value:2c
         * value:3d
         * value:4e
         */
        Disposable d = Flowable.zip(a, b, (aVal, bVal) -> aVal + bVal).subscribe(value -> LogUtils.i("value:" + value));
    }

    /**
     * 与concat的区别是concat没有交叉，merge可能会有交叉
     */
    private void merge() {
        Flowable<Long> a = Flowable.intervalRange(0, 10, 0, 1000, TimeUnit.MILLISECONDS);
        Flowable<Long> b = Flowable.intervalRange(100, 10, 500, 1000, TimeUnit.MILLISECONDS);

        //0,100,1,101...  。delayError，eager和concat一样
        Disposable d = Flowable.merge(a, b).subscribe(value -> LogUtils.i("" + value));
    }

    /**
     * 将Publishers串联，没有交织
     */
    private void concat() {
        Flowable<Integer> a = Flowable.just(1, 2, 3).delay(1000, TimeUnit.MICROSECONDS);
        Flowable<Integer> b = Flowable.just(4, 5, 6).delay(500, TimeUnit.MICROSECONDS);
        Flowable<Integer> error = Flowable.error(new Throwable("--->error<---"));
        //若中间发生错误，则调用onError结束执行，不再发射后面的数据
        Flowable.concat(a, b).subscribe(new Subscriber<Integer>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
                this.s = s;
            }

            @Override
            public void onNext(Integer integer) {
                //1,2,3,4,5,6
                //严格按照先后顺序发射数据，前一个Observable的数据没有发射完，后面Observable不发射数据
                LogUtils.i("concat:" + integer);
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {
                LogUtils.i("onError:" + t);
            }

            @Override
            public void onComplete() {

            }
        });
        /**
         * 与concat区别在于concat遇到error会调用onError结束执行，终止后面的发射，
         * 而concatDelayError会延迟error的发射，继续执行完后面的Publisher
         */
        Flowable.concatDelayError(Arrays.asList(a, error, b)).subscribe(new Subscriber<Integer>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                LogUtils.i("onNext:" + integer);
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {
                LogUtils.i("onError:" + t);
            }

            @Override
            public void onComplete() {

            }
        });

        /**
         *与concat基本逻辑一致，不同的是一旦合并后的Observable被订阅，
         * operator订阅所有的原始Observable，operator缓存每个Observable发射出来的值，所有Observable发射完成之后，operator将所有缓存的值按顺序一次排放出来。
         */
        Flowable.concatEager(Arrays.asList(a, b), 1, 1).subscribe(new Subscriber<Integer>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                LogUtils.i("onNext:" + integer);
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }


    /**
     * <d>NOTE:</d>
     * 其中一个Observable发射数据项后，
     * 组合所有Observable发射他们的最后一个数据项（前提是所有的Observable都至少发射过一个数据项）
     */
    private void combineLatest() {
        Flowable<String> phoneFlowable = Flowable.create(emitter -> etPhone.addTextChangedListener(new EditTextMonitor(emitter))
                , BackpressureStrategy.BUFFER);

        Flowable<String> pwdFlowable = Flowable.create(emitter -> etPwd.addTextChangedListener(new EditTextMonitor(emitter))
                , BackpressureStrategy.BUFFER);

/*        Disposable d = Flowable.combineLatestDelayError(objects -> "1234", phoneFlowable, pwdFlowable)
                .doFinally(() -> {

                }).subscribe(s -> {

                });*/

        Flowable.combineLatest(phoneFlowable, pwdFlowable, (s1, s2) -> s1 + "\n" + s2)
                .subscribe(new Subscriber<String>() {
                    Subscription s;

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                        this.s = s;
                    }

                    @Override
                    public void onNext(String s) {
                        LogUtils.i("combineLatest:" + s);
                        this.s.request(1);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private class EditTextMonitor implements TextWatcher {
        private FlowableEmitter<String> emitter;

        public EditTextMonitor(FlowableEmitter<String> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            emitter.onNext(s.toString());
        }
    }
}
