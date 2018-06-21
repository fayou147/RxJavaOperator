package com.fy.operator.item;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;

import com.fy.operator.LogUtils;
import com.fy.operator.R;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class BlockingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocking);
        blockingSubscribe();
    }

    /**
     * 阻塞直到Observable发射了一个数据，然后返回第一项数据
     * <em>区别是阻塞值得第一个值到达并return</em>
     */
    private void blockingFirst() {
        int a = Flowable.just(1, 2, 3).blockingFirst();
        // 1
        LogUtils.i("blockingFirst:" + a);
    }

    /**
     * 对Flowable发射的每一个数据调用一个方法，并且阻塞直到Flowable完成
     * <p>
     * <em>NOTE:</em>  即使下层的flowable是异步的，这也会阻塞
     */
    private void blockingForEach() {
        Flowable.just(1, 2, 3).blockingForEach(value -> {
            //1,2,3
            LogUtils.i("blockingForEach:" + value);
        });
    }

    /**
     * 返回Flowable发射的最后一个item,并且会阻塞直到发射完成
     */
    private void blockingLast() {
        int a = Flowable.just(1, 2, 3).blockingLast();
        LogUtils.i("blockingLast:" + a);
    }

    /**
     * return一个Iterable，Iterable返回由这个Flowable发射的最新item.
     * <p>
     * 如果Flowable产生的item比Iterator.next快，onNext事件可能被跳过，Iterator就没有事件take.最终Iterator
     * 中没有数据
     * </p>
     * <em>NOTE:</em>如果onNext紧跟随onComplete,可能会隐藏onNext活动，即最后发射的一个item可能隐藏掉
     */
    private void blockingLatest() {
        Flowable<Long> source = Flowable.interval(1, TimeUnit.SECONDS).take(10);
        Iterable<Long> iterable = source.blockingLatest();
        Iterator<Long> it = iterable.iterator();
        while (it.hasNext()) {
            LogUtils.i("blockingLatest:" + it.next());
        }
    }

    /**
     * return一个Iterable，这个Iterable总是返回由这个Flowable最近发射的item
     * <p>
     * <p>
     * initialItem  初始化item. 调用blockingMostRecent生成Iterable后
     * 如果Flowable还没有发射一个item,那么Iterable将生成初始item
     * <p>
     * return一个Iterable，在每次迭代(it.next)中返回这个flowable最近发射的item
     * </p>
     */
    private void blockingMostRecent() {
        FlowableProcessor<String> s = PublishProcessor.create();

        Iterator<String> it = s.blockingMostRecent("default").iterator();
//        s.onNext("one");
        LogUtils.i("hasNext:" + it.hasNext());  //true
        LogUtils.e("###:" + it.next());  //default
        LogUtils.e("###:：" + it.next());  //default
        LogUtils.i("hasNext:" + it.hasNext()); //true
        s.onNext("two");
        LogUtils.i("hasNext:" + it.hasNext()); //true
        LogUtils.e("###:：" + it.next()); //two

    }

    /**
     * return一个Iterable，在每次跌倒之前阻塞直到Flowable发射了另一个item。
     */
    private void blockingNext() {
        PublishProcessor<String> processor = PublishProcessor.create();
        Iterator<String> it = processor.blockingNext().iterator();
        fireOnNextInNewThread(processor, "one");
        LogUtils.i("one:" + it.hasNext());  //true
        LogUtils.e("###:" + it.next());  //one
        //间隔500sm
        fireOnNextInNewThread(processor, "two");
        LogUtils.i("two:" + it.hasNext());  //true
        LogUtils.e("###:" + it.next());  //two

        fireOnNextInNewThread(processor, "three");
        try {
            LogUtils.e("###:" + it.next());  //three
        } catch (NoSuchElementException e) {
            LogUtils.e("Calling next() without hasNext() should wait for next fire");
        }

        processor.onComplete();
        LogUtils.i("onComplete:" + it.hasNext());  //false
        try {
            it.next();

        } catch (NoSuchElementException e) {
            LogUtils.e("At the end of an iterator should throw a NoSuchElementException");
        }

        // If the observable is completed, hasNext always returns false and next always throw a NoSuchElementException.
        LogUtils.i("onComplete:" + it.hasNext());  //false
        try {
            it.next();

        } catch (NoSuchElementException e) {
            LogUtils.e("At the end of an iterator should throw a NoSuchElementException");
        }
    }

    private void fireOnNextInNewThread(final FlowableProcessor<String> o, final String value) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
                o.onNext(value);
            }
        }.start();
    }

    /**
     * 如果flowable在发射完单个item后完成，则返回这个item。如果发射的item超过一个，抛出IllegalArgumentException异常
     * 如果没有发射元素，返回一个default值
     */
    private void blockingSingle() {
        // 1
        Flowable<String> empty = Flowable.empty();
        String c = empty.blockingSingle("c");  //c
        // 2
        Flowable<String> single = Flowable.just("a");
        String a = single.blockingSingle();  //a
        // 3
        Flowable<String> error = Flowable.just("a", "b");
        String err = error.blockingSingle();  //IllegalArgumentException
    }

    /**
     * 订阅源在当前的线程上调用给定的回调函数。
     * 更能类似blockingForEach
     * <p>
     * 调用该方法将阻塞调用者线程，直到上游正常终止或有错误。因此不建议特殊线程（android主线程，Swing事件分发线程）调用这个方法
     * </p>
     */
    private void blockingSubscribe() {
        Flowable
                .interval(500, TimeUnit.MILLISECONDS)
                .take(10)
                .subscribeOn(Schedulers.io())
                .blockingSubscribe(strVal -> {
                    //会阻塞，直到发射下一个item,间隔500ms
                    LogUtils.i("blockingSubscribe:" + strVal + ",getName:" + Thread.currentThread().getName());
                }, error -> {

                }, () -> {
                    //onComplete
                });
    }
}
