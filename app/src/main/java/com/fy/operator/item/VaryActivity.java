package com.fy.operator.item;

import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;

import com.fy.operator.LogUtils;
import com.fy.operator.R;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Notification;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class VaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vary);
        window();
    }

    /**
     * 返回一个Flowable，他发射从源Flowable缓存的item集合
     * <em>NOTE:</em>将Flowable中的数据分成count长度的集合，每次向后移动skip位
     */
    private void buffer() {
        Flowable.just(1, 2, 3, 4, 5).buffer(3, 2).subscribe(new Subscriber<List<Integer>>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
                this.s = s;
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onNext(List<Integer> integers) {
                LogUtils.e("onNext");
                for (Integer integer : integers) {
                    LogUtils.i("buffer:" + integer);
                }
                s.request(1);
                //[1,2,3]
                //[3,4,5]
                //[5]
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                LogUtils.i("onComplete");
            }
        });
    }

    /**
     * 返回一个Flowable，将源Flowable发射的item转换成指定类型再发射
     */
    private void cast() {
        Flowable<?> flowable = Flowable.just(1, 2);
        Flowable<Integer> a = flowable.cast(Integer.class);
        Disposable d = a.subscribe(value -> {
            LogUtils.e("cast:" + value);
        });
    }

    /**
     * 将一个被观察者转换成另外一种被观察者，在转化过程中不是安装源Flowable发射的顺序转化的
     * 与此对应的是concatMap(转化的被观察者是按照源发射的顺序)
     */
    private void flatMap() {
        Disposable d = Flowable.just(1, 2, 3)
                .flatMap(integer -> {
                    Flowable<String> flowable;
                    if (integer == 2) {
                        flowable = Flowable.just(String.valueOf(integer * 10)).delay(500, TimeUnit.MILLISECONDS);
                    } else {
                        flowable = Flowable.just(String.valueOf(integer * 10));
                    }
                    return flowable;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> LogUtils.i("flatMap：" + s)); //10,30,20.
    }


    /**
     * 将一个发射数据的Flowable变换为多个Flowables，与flatMap区别就是concatMap转换是顺序的，flatMap可能会无序
     */
    private void concatMap() {
        Disposable d = Flowable.just(1, 2, 3)
                .concatMap(integer -> {
                    Flowable<String> flowable;
                    if (integer == 2) {
                        flowable = Flowable.just(String.valueOf(integer * 10)).delay(500, TimeUnit.MILLISECONDS);
                    } else {
                        flowable = Flowable.just(String.valueOf(integer * 10));
                    }
                    return flowable;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> LogUtils.i("concatMap：" + s)); //10，20，30.如果改为flapMap结果则为10，30，20
    }

    /**
     * materialize把 被观察者Flowable转换为Notification通知对象。
     */
    private void materialize() {
        Disposable d = Flowable.just(1, 2).materialize().subscribe(new Consumer<Notification<Integer>>() {
            @Override
            public void accept(Notification<Integer> integerNotification) throws Exception {
                if (integerNotification.isOnNext()) {
                    LogUtils.i("materialize:" + integerNotification.getValue());  //1,2
                }
            }
        });
    }

    /**
     * 与materialize相反，将Notification通知转换成Flowable
     */
    private void dematerialize() {
        Flowable<Notification<Integer>> notifications = Flowable.just(1, 2).materialize();
        Disposable d = notifications.dematerialize().subscribe(value -> {
            LogUtils.i("value:" + value);
        });
    }

    /**
     * 应用一个函数来变换Flowable发射的每一个item
     */
    private void map() {
        Disposable d = Flowable
                .interval(1000, TimeUnit.MILLISECONDS)
                .map(new Function<Long, String>() {
                    @Override
                    public String apply(Long aLong) throws Exception {
                        return "str:" + aLong;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> LogUtils.e("map:" + value));
    }

    /**
     * 强制使Flowable的发射和通知服从Flowable合约（即onComplete之后不再发射onNext）
     */
    private void serialize() {
        Flowable<Integer> source = Flowable.create(emitter -> {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SystemClock.sleep(100);
                    emitter.onComplete();
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        emitter.onNext(1);
                    }
                }
            }).start();
        }, BackpressureStrategy.DROP);
        /**
         * 不加serialize时有概率出现onComplete之后另一个线程还在发射数据的现象
         * ...
         * onComplete:
         * doFinally
         * onNext:1
         */
        source.serialize()
                .doFinally(() -> {
                    LogUtils.e("doFinally");
                })
                .safeSubscribe(new Subscriber<Integer>() {
                    Subscription s;

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                        this.s = s;
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
                        LogUtils.w("onComplete:");
                    }
                });
    }

    /**
     * switchMap用法与flatMap类似，但是转换出来的每一个新的数据（被观察者）发射会取代掉前一个被观察者
     */
    private void switchMap() {
        Flowable<String> flowable = Flowable.just("A", "B", "C", "D");
        Disposable d = flowable
                .switchMap(s -> Flowable.just("switch " + s).delay(1, TimeUnit.SECONDS))
                .subscribe(this::print);  //switch D
    }

    private void to() {
        String a = Flowable.just(1, 2, 3).to(new Function<Flowable<Integer>, String>() {
            @Override
            public String apply(Flowable<Integer> integerFlowable) throws Exception {
                return "str:" + integerFlowable.blockingSingle();
            }
        });
    }

    /**
     * 上游发射item转换成list
     */
    private void toList() {
        Disposable d = Flowable.just(1, 2, 3).toList().subscribe(this::print);
    }

    /**
     * 上游发射item转换成Map
     */
    private void toMap() {
        Disposable d = Flowable.just(1, 2, 3).toMap(new Function<Integer, String>() {
            @Override
            public String apply(Integer integer) throws Exception {
                return "str" + integer;
            }
        }).subscribe(this::print); // {str2=2, str1=1, str3=3}
    }

    private void toMultimap() {
        Flowable.just(1, 2, 3).toMultimap(new Function<Integer, String>() {
            @Override
            public String apply(Integer integer) throws Exception {
                return "str" + integer;
            }
        }).subscribe(new SingleObserver<Map<String, Collection<Integer>>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(Map<String, Collection<Integer>> stringCollectionMap) {

            }

            @Override
            public void onError(Throwable e) {

            }
        }); // {str2=[2], str1=[1], str3=[3]}
    }

    /**
     * 首先会立即打开它的第一个窗口，原Flowable每发射skip项数据它就打开一个新窗口
     * 每当当前窗口发射了count项数据，它就关闭当前窗口并打开一个新窗口
     * 如果skip < count，窗口可会有count - skip 个重叠的数据；
     * 如果skip > count，在两个窗口之间会有skip - count项数据被丢弃。
     */
    private void window() {
        Disposable d = Flowable.range(0, 10)
                .window(3, 2)
                .subscribe(new Consumer<Flowable<Integer>>() {
                    @Override
                    public void accept(Flowable<Integer> integerFlowable) throws Exception {
                        Disposable d = integerFlowable.subscribe(VaryActivity.this::print, VaryActivity.this::print, () -> {

                            LogUtils.w("====================================");
                        });
                    }
                });

    }

    private <T> void print(T t) {
        LogUtils.e("#" + t.getClass().getSimpleName() + ":" + t);
    }
}
