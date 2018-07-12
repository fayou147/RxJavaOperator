package com.fy.operator.item;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.fy.operator.LogUtils;
import com.fy.operator.R;

import org.intellij.lang.annotations.Flow;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.GroupedFlowable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
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
        withLatestFrom();
    }

    /**
     * scan和reduce都是把操作的观察序列的上一次操作的结果做为第二次的参数传递给累加函数，
     * 但两者有区别:scan:每次累计后会订阅输出结果，然后在调用累加函数进行第二次操作
     * 即reduce最后调用一次subscribe，而scan每次累加完都会先调用一次subscribe，输出结果
     */
    private void scan() {
        /**
         * scan:one
         * s:one,integer:1
         * scan:one1
         * s:one1,integer:2
         * scan:one12
         * s:one12,integer:3
         * scan:one123
         * s:one123,integer:4
         * scan:one1234
         * s:one1234,integer:5
         * scan:one12345
         */
        Disposable d = Flowable.range(1, 5)
                .scan("one", new BiFunction<String, Integer, String>() {
                    @Override
                    public String apply(String s, Integer integer) throws Exception {
                        LogUtils.i("s:" + s + ",integer:" + integer);
                        return s + integer;
                    }
                })
                .subscribe(value -> LogUtils.i("scan:" + value)); //one12345
    }

    /**
     * 将flowable发射的第一个item应用与一个规定的累加函数，
     * 然后将该函数的返回值与Flowable发出的第二个item作为参数传递给该函数，以此类推
     */
    private void reduce() {
        Disposable d = Flowable
                .range(1, 5)
                .reduce("one", new BiFunction<String, Integer, String>() {
                    @Override
                    public String apply(String s, Integer integer) throws Exception {
                        //s:one,integer:1
                        //s:one1,integer:2
                        //s:one12,integer:3
                        //s:one123,integer:4
                        //s:one1234,integer:5

                        LogUtils.i("s:" + s + ",integer:" + integer);
                        return s + integer;
                    }
                })
                .subscribe(value -> LogUtils.i("reduce:" + value)); //one12345
    }

    /**
     * 将原始Observable发射的数据放到一个单一的可变的数据结构中，然后返回一个发射这个数据结构的Observable
     * <em>NOTE:</em>需要自己定义收集的容器和收集逻辑
     */
    private void collect() {
        Single<List<Integer>> s = Flowable.just(1, 2, 3, 4).collect(() -> new ArrayList<>(),  //创建收集容器
                (list, integer) -> list.add(integer)); //收集逻辑

//        List<Integer> container = new ArrayList<>();
//        Flowable.just(1, 2, 3, 4).collectInto(container, (list, integer) -> list.add(integer));

        s.subscribe(new SingleObserver<List<Integer>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(List<Integer> integers) {
                for (Integer integer : integers) {
                    LogUtils.e("collect onSuccess:" + integer);
                }
                //[1,2,3,4]
            }

            @Override
            public void onError(Throwable e) {

            }
        });
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
     * 仅当source Publisher 发射item时，才会使用函数将指定的Publisher合并到source序列中
     */
    private void withLatestFrom() {
        Flowable<Long> source = Flowable.intervalRange(0, 10, 0, 1, TimeUnit.SECONDS);
        Flowable<String> other = Flowable.just("one", "two", "three").delay(1500, TimeUnit.MILLISECONDS);
        Disposable d = source.withLatestFrom(other, new BiFunction<Long, String, String>() {
            @Override
            public String apply(Long aLong, String s) throws Exception {
                return s + aLong;
            }
        }).subscribe(this::print);  //three2, three3, three4, three5, three6, three7,three8,three9
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

    /**
     * 将原始Observable发射的数据按Key分组，每一个Observable发射一组不同的数据。
     */
    private void groupBy() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five", "six");
        //创建分组的key
        Function<String, Integer> key = value -> value.length();

        Flowable<GroupedFlowable<Integer, String>> grouped = source.groupBy(key);

        grouped.blockingForEach(new Consumer<GroupedFlowable<Integer, String>>() {
            @Override
            public void accept(GroupedFlowable<Integer, String> integerStringGroupedFlowable) throws Exception {
                LogUtils.e("getKey:" + integerStringGroupedFlowable.getKey());
                /**
                 * <3,one>   <3,two>   <3,six>
                 * <5,three>
                 * <4,four>
                 */
                Disposable d = integerStringGroupedFlowable.subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        LogUtils.i("groupBy:" + s);
                    }
                });
            }
        });
    }

    /**
     * 将两个Flowable中发射的数据在它们各自的有效期内进行组合,语法结构大致是:
     * flowableA.join(flowableB, 控制flowableA发射数据有效期的函数， 控制flowableB发射数据有效期的函数，
     * 两个flowable发射数据的合并规则)
     *
     * @param other          目标Flowable,与源Flowable向组合的
     * @param leftEnd        接收从源Flowable发射来的数据，并返回一个Publisher，
     *                       这个Publisher的声明周期决定了源Flowable发射出来的数据的有效期；
     * @param rightEnd       接收目标Flowable发射来的数据，并返回一个Publisher，
     *                       这个Publisher的声明周期决定了目标Flowable发射出来的数据的有效期；
     * @param resultSelector 接收从源Flowable和目标Flowable发射出来的数据，并将这两个数据组合后返回。
     *                       <p>
     *                       join操作符的效果类似于排列组合，把第一个数据源A作为基座窗口，他根据自己的节奏不断发射数据元素，
     *                       第二个数据源B，每发射一个数据，我们都把它和第一个数据源A中已经发射的数据进行一对一匹配；举例来说，
     *                       如果某一时刻B发射了一个数据“B”,此时A已经发射了0，1，2，3共四个数据，
     *                       那么我们的合并操作就会把“B”依次与0,1,2,3配对，得到四组数据： [0, B][1, B] [2, B] [3, B]
     */
    private void join() {
        LogUtils.e("<------start");
        Flowable<String> source = Flowable.interval(100, TimeUnit.MILLISECONDS).map(i -> "L" + i);

        Flowable<String> other = Flowable.interval(100, TimeUnit.MILLISECONDS).map(i -> "R" + i);
        Disposable d = source
                .join(other,
                        left -> Flowable.timer(150, TimeUnit.MILLISECONDS),
                        right -> Flowable.timer(0, TimeUnit.MILLISECONDS),
                        (s, s2) -> s + " - " + s2

                )
                .take(10)
                .subscribe(value -> LogUtils.i("join:" + value));
        /**
         * <em>NOTE:</em> source与other没隔100ms发射一个数据/
         * source发射的数据有效期为150ms,1——(100ms)——2——(100ms)——3 150m后，数据1已经产生了150ms,因此失效，剩下数据2，3和other组合
         * other发射的数据有效期为0ms，即发射完成后立即与source有效数据组合，组合完成后即失效
         * L0-R0  R0失效，继续发射L1,R1,因为L0还没失效
         * L0-R1,L1-R1   R1失效,再发射L2,R2，因为L0从开始产生到现在已经超过150ms,所以L0失效
         * L1-R2,L2-R2
         *
         */
    }


    private void groupJoin() {
        LogUtils.e("<------start");
        Flowable<String> source = Flowable.interval(100, TimeUnit.MILLISECONDS).map(i -> "L" + i);

        Flowable<String> other = Flowable.interval(100, TimeUnit.MILLISECONDS).map(i -> "R" + i);
        Disposable d = source
                .groupJoin(other,
                        left -> Flowable.timer(150, TimeUnit.MILLISECONDS),
                        right -> Flowable.timer(0, TimeUnit.MILLISECONDS),
                        (sl, sr) -> new GroupJoinTest(sl, sr))
                .take(10)
                .subscribe(new Consumer<GroupJoinTest>() {
                    @Override
                    public void accept(GroupJoinTest groupJoinTest) throws Exception {
                        groupJoinTest.getRightFlow().forEach(s -> LogUtils.e(groupJoinTest.getLeftStr() + "-" + s));
                    }
                });
    }

    class GroupJoinTest {
        private String leftStr;
        private Flowable<String> rightFlow;

        public GroupJoinTest(String leftStr, Flowable<String> rightFlow) {
            this.leftStr = leftStr;
            this.rightFlow = rightFlow;
        }

        public String getLeftStr() {
            return leftStr;
        }

        public void setLeftStr(String leftStr) {
            this.leftStr = leftStr;
        }

        public Flowable<String> getRightFlow() {
            return rightFlow;
        }

        public void setRightFlow(Flowable<String> rightFlow) {
            this.rightFlow = rightFlow;
        }
    }

    private <T> void print(T t) {
        LogUtils.e("#" + t.getClass().getSimpleName() + ":" + t);
    }

}
