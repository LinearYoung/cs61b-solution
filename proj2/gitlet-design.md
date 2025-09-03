# Gitlet 设计文档

# 总言
- 这是一个简易版的git，代码量大概在2000行左右。无论是对于熟悉数据结构，还是锻炼工程能力，都可以称得算是相当优质的作业项目。
- 想更深入了解Git，最好查阅Git的官方文档proGit
- 这是UC Berkeley CS61B的一个课程项目，项目说明文档：https://sp21.datastructur.es/materials/proj/proj2/proj2#global-log
- 诚挚感谢UC Berkeley以及各位教师，TAs对该项目以及课程的精心设计和无私开源。
- 由于本人水平堪忧，第一次面对如此项目，有点无从下手，不知道如何实现。有参考github上前辈的实现方案。
- 最大的感受是，并不像之前proj1中，完整写好一个类后调用。很多时候是这个文件写一半，发现要调用的另外一个文件的函数还没写，于是转头去写另外一个。
- 所以建议是，可以先尝试简单，易实现的模块先写，比如main中的init等等。发现需要的函数，类还没构造就顺着去构造。这样就自然而然地找出了一条实现的道路。
- 明确两个map的对应关系， 一个是文件名 --> 版本号（sha1）， 另一个是 版本号 sha1 --> 文件内容
- 明确好commit，stage和CWD之间的关系，也就是git是怎么追踪这三者之间文件的差别以及在add和commit中怎么做到合并的。
- 因为本项目简化为只在根目录工作（无嵌套子目录），故实现略去tree实现。tree实现思路如下：将文件目录作为树节点，目录中所带文件的指针作为节点所带内容。目录也有自己的子目录（子树节点）。每一个commit对应一个tree。
- Utils类同广大程序员的核心使命在于增删改查



## 主要工具类
因为懒，介绍一些重要的类以及将着重介绍一些个人认为比较重要的函数或者操作逻辑

### Main
读取命令并调用相应指令  
实现commandRunner，指令运行的模块化  
利用Runnable和java.util.function.Consumer(BiConsumer)实现将指令作为参数变量传入;
#### Commit
一个公式化java bean类

### CommitUtils
主要函数  
1. isConsistent， 判断一个文件对于两个commit是否同时不跟踪，或跟踪且内容一致
2. commitTraceBack, 只追溯parentCommit到initialCommit，返回遍历的commit列表
3. commitAncestors，同时追溯parentCommit和secondParentCommit，返回遍历的commit列表
4. getSplitCommit， 找到两条分支的分离点，做法是对两分支的末commit进行commitTraceBack，生成的序列倒序。在第一个两序列不同的提交的前一项即为分离点
5. getSplitCommitWithGraph。用commitAncestors找到两分支末commit的所有祖先，并生成公共祖先。利用提交时间必定增长的性质，返回公共祖先中提交时间最大的那一个
6. createObjectFile,对比新旧提交，并把发生变化的文件写入磁盘持久化。(注:该实现与gitlet设计文档不同，比如A -> B -> C的同名文件提交， 若a，b不同，a，c相同，就会造成c复写一遍而不是利用已有的a文件。应该写一个判断逻辑)


原谅主播，感觉每个工具类的核心点都在增删改查，如有需要可以去看方法注释不再过多阐述，我懒。

### merge
难点在于冲突合并
1. 用getSplitCommit找到公共祖先
2. 检查当前，分支，以及分离点的commit文件来判断操作
