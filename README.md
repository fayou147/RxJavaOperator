# RxJavaOperator
sort out RxJava2  various operators

## Create

* create
* defer 延缓，订阅之后被观察者才创建
* empty
* error
* fromArray
* fromCallable
* fromFuture
* fromIterable
* fromPublisher
* generate
* just
* interval 定时发射
* intervalRange
* range  范围
* timer  延迟xxx时间发射

## Filter(过滤)

* amb 发射首先发射的item
* all 所有条件满足返回true
* all 任意一个item满足条件true

## Merge(聚合)

* zip
* merge 可能有交叉
* concat 串联
* combineLatest 其中一个Observable发射数据项后，组合所有Observable发射他们的最后一个数据项

## Block(阻塞)

* blockingFirst  阻塞直到Observable发射了一个数据，然后返回第一项数据
* blockingForEach 发射的每一个item会阻塞
* blockingLast
* blockingLatest  返回一个Iterable，里面是最新发射的item,在发射下一个之前会阻塞
* blockingMostRecent  类似blockingLatest
* blockingNext  每次迭代之前阻塞直到Flowable发射了另一个item
* blockingSingle  返回这个item
* blockingSubscribe  类似blockingForEach
