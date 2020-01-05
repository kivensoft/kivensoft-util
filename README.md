# kivensoft-util
util tools class from kivensoft.
一些常用通用的辅助工具类, 减少编码工作量

### Bits
二进制 <-> 十六进制 之间的转换和测试比对辅助工具类

### Cache
缓存接口, 系统默认实现LRUCache和WeakCache两种

### Crypt
口令加密接口, 目前已实现兼容unix的crypt md5加密和bcrypt加密算法

### FastBuffer
动态扩展的缓存类, 以1024byte为单位动态扩展

### FastThrowable
快速异常类, 不创建异常堆栈

### Fmt
格式化到动态字符串缓冲区的类，采取缓存方式实现，增强对日期、数组、列表的格式化

### IntArray
int数组类, 动态扩展

### IntNumber
可修改的IntNumber类, 适用于lambda表达式中修改不可变对象

### KeyValue
键值对象, 整数为键, 字符串为值

### Langs
基于jdk8的实用函数集合

### Logx
扩展日志类，基于slf4j/logback实现，增强动态参数, 使用自带的高效格式化函数

### LongNumber
可修改的LongNumber类, 适用于lambda表达式中修改不可变对象

### Mapx
增强map类, 添加类型转换函数

### ObjectPool
对象池接口, 实现对可复用对象的对象池管理, 线程安全, 采取非锁方式实现

### ObjRef
对象引用类, 适用于lambda函数中的可修改对象

### Pair
键值对泛型类

### Resources
针对系统路径和jar包进行统一资源处理的资源处理类

### ScanPackage
获取指定包下的所有类，自动循环扫描下级目录

### SQL
动态生成SQL语句类

### StampedLockx
乐观读写锁扩展子类, 扩展了三个常见场景的读写函数, 适用于读多写少的情况

### Strings
字符串增强处理类, 提供众多的字符串处理辅助函数

### TextBuilder
基于Javolution(http://javolution.org/)的TextBuilder改造而来的类
采用二维数组方式, 实现动态扩展无需频繁复制, 比SringBuilder好用

### Tuple
元数据对象类, 提供1-6个参数的接口实现类
