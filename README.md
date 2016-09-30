# SqlDelightSqlBriteDemo


####SqlBrite 和 SqlDelight 的简介
1. SqlBrite
>A lightweight wrapper around SQLiteOpenHelper and ContentResolver which introduces reactive stream semantics to queries.
>SQLBrite是对SQLiteOpenHelper和ContentResolver的封装, 对请求操作引入了响应式语义（Rx）（用来在 RxJava 中使用）

2. SqlDelight
>SQLDelight generates Java models from your SQL CREATE TABLE statements. These models give you a typesafe API to read & write the rows of your tables. It helps you to keep your SQL statements together, organized, and easy to access from Java.
>SQLDelight 通过SQL语句生成Java模型代码, 生成的Java模型会提供类型安全的API来对表进行操作. 这样做的好处是你的SQL语句都在一个位置, 通过查看 SQL 语句可以清楚的了解需要实现的功能和数据库的结构,也便于管理以及java类访问.

####SqlBrite的使用
>Create a SqlBrite instance which is an adapter for the library functionality.

创建一个SqlBrite对象, 该对象是该库的功能入库:

```
SqlBrite sqlBrite = SqlBrite.create(); 
```

>Pass a SQLiteOpenHelper instance and a Scheduler to create a BriteDatabase.

提供一个SQLiteOpenHelper对象和一个Scheduler对象来创建一个BriteDatabase对象.

```
BriteDatabase db = sqlBrite.wrapDatabaseHelper(openHelper, Schedulers.io());
```

>A Scheduler is required for a few reasons, but the most important is that query notifications can trigger on the thread of your choice. The query can then be run without blocking the main thread or the thread which caused the trigger.

Scheduler对象最大的作用是可以指定执行查询操作的线程, The query can then be run without blocking the main thread or the thread which caused the trigger.

>The BriteDatabase.createQuery method is similar to SQLiteDatabase.rawQuery except it takes an additional parameter of table(s) on which to listen for changes. Subscribe to the returned Observable<Query> which will immediately notify with a Query to run.

BriteDatabase.createQuery方法和SQLiteDatabase.rawQuery方法相比多了一个table(s)参数, 用于监听数据变化. 当我们订阅subscribe返回的Observable<Query>的时候，立刻执行需要的查询语句。

```
Observable<Query> users = db.createQuery("users", "SELECT * FROM users");
users.subscribe(new Action1<Query>() {
  @Override public void call(Query query) {
    Cursor cursor = query.run();
    // TODO parse data...
  }
});
```

>Unlike a traditional rawQuery, updates to the specified table(s) will trigger additional notifications for as long as you remain subscribed to the observable. This means that when you insert, update, or delete data, any subscribed queries will update with the new data instantly.

和传统的 rawQuery 方法不同的是, 只要你订阅了observable , 在你插入更新或修改数据的时候, 订阅的查询操作都将得到数据的更新, 下面这段代码示范了数据通知的操作.

```
final AtomicInteger queries = new AtomicInteger();
users.subscribe(new Action1<Query>() {
  @Override public void call(Query query) {
    queries.getAndIncrement();
  }
});
System.out.println("Queries: " + queries.get()); // Prints 1

db.insert("users", createUser("jw", "Jake Wharton"));
db.insert("users", createUser("mattp", "Matt Precious"));
db.insert("users", createUser("strong", "Alec Strong"));

System.out.println("Queries: " + queries.get()); // Prints 4
```

>In the previous example we re-used the BriteDatabase object "db" for inserts. All insert, update, or delete operations must go through this object in order to correctly notify subscribers.
>Unsubscribe from the returned Subscription to stop getting updates.

sqlbrite 使用这个表的名字来通知其他监听该表数据的 Observable 对象来更新数据。这就要求你只能通过 BriteDatabase 来访问数据库， 而不能使用 SQLiteOpenHelper 。 
下面代码示范了通过Subscription取消订阅之后, 将不会再接收到数据的操作通知.

```
final AtomicInteger queries = new AtomicInteger();
Subscription s = users.subscribe(new Action1<Query>() {
  @Override public void call(Query query) {
    queries.getAndIncrement();
  }
});
System.out.println("Queries: " + queries.get()); // Prints 1

db.insert("users", createUser("jw", "Jake Wharton"));
db.insert("users", createUser("mattp", "Matt Precious"));
s.unsubscribe();

db.insert("users", createUser("strong", "Alec Strong"));

System.out.println("Queries: " + queries.get()); // Prints 3
```

>Use transactions to prevent large changes to the data from spamming your subscribers.

如果提交大量数据，则可以使用事务处理：

```
final AtomicInteger queries = new AtomicInteger();
users.subscribe(new Action1<Query>() {
  @Override public void call(Query query) {
    queries.getAndIncrement();
  }
});
System.out.println("Queries: " + queries.get()); // Prints 1

Transaction transaction = db.newTransaction();
try {
  db.insert("users", createUser("jw", "Jake Wharton"));
  db.insert("users", createUser("mattp", "Matt Precious"));
  db.insert("users", createUser("strong", "Alec Strong"));
  transaction.markSuccessful();
} finally {
  transaction.end();
}

System.out.println("Queries: " + queries.get()); // Prints 2
```
>注意: 还可以在 Transaction 对象上用 try-with-resources

>Since queries are just regular RxJava Observable objects, operators can also be used to control the frequency of notifications to subscribers.

由于查询只是普通的 RxJava Observable 对象, 操作符可以用于控制通知subscribers的频率.

```
users.debounce(500, MILLISECONDS).subscribe(new Action1<Query>() {
  @Override public void call(Query query) {
    // TODO...
  }
});
```

>The SqlBrite object can also wrap a ContentResolver for observing a query on another app's content provider.

SqlBrite也可以通过封装一个ContentResolver用于观察其他app的 content provider 的数据查询操作.

```
BriteContentResolver resolver = sqlBrite.wrapContentProvider(contentResolver, Schedulers.io());
Observable<Query> query = resolver.createQuery(/*...*/);
```

>The full power of RxJava's operators are available for combining, filtering, and triggering any number of queries and data changes.

RxJava的操作符在混合，过滤，和触发任意数量的查询和数据的变化的时候能发挥很大的作用。

####SqlDelight
1. 示例
>To use SQLDelight, put your SQL statements in a .sq file, like src/main/sqldelight/com/example/HockeyPlayer.sq. Typically the first statement creates a table.

要使用 SQLDelight , 需要把你的 SQL 语句放到相应的 `.sq` 文件中, 例如 `src/main/sqldelight/com/example/HockeyPlayer.sq`. 在 .sq 文件中一般第一个语句是创建表的语句:

```
CREATE TABLE hockey_player (
  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  number INTEGER NOT NULL,
  name TEXT NOT NULL
);

-- 其他的语句通过标识符来引用。在生成的 Java 对象中会包含
-- 一个该标识符的常亮引用这个语句。
select_by_name:
SELECT *
FROM hockey_player
WHERE name = ?;
```

>From this SQLDelight will generate a HockeyPlayerModel Java interface with nested classes for reading (the Mapper) and writing (the Marshal) the table.

上面的 SQL 语句会生成一个 HockeyPlayerModel Java 接口。该接口内有两个嵌套类分别把 Cursor 映射为 Java 对象以及把 Java 对象转换为 ContentValues 好插入数据库，这两个嵌套类分别称之为 Mapper 和 Marshal：

```
package com.example;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

public interface HockeyPlayerModel {
  String TABLE_NAME = "hockey_player";

  String _ID = "_id";

  String NUMBER = "number";

  String NAME = "name";

  String CREATE_TABLE = ""
      + "CREATE TABLE hockey_player (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  number INTEGER NOT NULL,\n"
      + "  name TEXT NOT NULL\n"
      + ")";

  String SELECT_BY_NAME = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "WHERE name = ?";

  long _id();

  long number();

  @NonNull
  String name();

  interface Creator<T extends HockeyPlayerModel> {
    T create(long _id, long number, String name);
  }

  final class Mapper<T extends HockeyPlayerModel> implements RowMapper<T> {
    private final Factory<T> hockeyPlayerModelFactory;

    public Mapper(Factory<T> hockeyPlayerModelFactory) {
      this.hockeyPlayerModelFactory = hockeyPlayerModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return hockeyPlayerModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getLong(1),
          cursor.getString(2)
      );
    }
  }

  class Marshal<T extends Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    Marshal(@Nullable HockeyPlayerModel copy) {
      if (copy != null) {
        this._id(copy._id());
        this.number(copy.number());
        this.name(copy.name());
      }
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }

    public T number(long number) {
      contentValues.put(NUMBER, number);
      return (T) this;
    }

    public T name(String name) {
      contentValues.put(NAME, name);
      return (T) this;
    }
  }

  final class Factory<T extends HockeyPlayerModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Mapper<T> select_by_nameMapper() {
      return new Mapper<T>(this);
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(HockeyPlayerModel copy) {
      return new Marshal(copy);
    }
  }
}
```

2. AutoValue
>Using Google's AutoValue you can minimally make implementations of the model/marshal/mapper:

使用 Google's AutoValue 你可以大大减少实现这个接口需要编写的代码量:

```
AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(new Creator<HockeyPlayer>() {
    @Override public HockeyPlayer create(long _id, long number, String name) {
      return new AutoValue_HockeyPlayer(_id, age, number, gender);
    }
  });

  public static final RowMapper<HockeyPlayer> MAPPER = FACTORY.select_by_nameMapper();
}
```

>If you are also using Retrolambda the anonymous class can be replaced by a method reference:

配合使用 Retrolambda 匿名内部类可以被替换为一个方法接口

```
AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(AutoValue_HockeyPlayer::new);

  public static final RowMapper<HockeyPlayer> MAPPER = FACTORY.select_by_nameMapper();
}
```

3. Consuming Code
>Use the generated constants to reference table names and SQL statements.

使用生成的常量引用表名和 SQL 语句

```
public void insert(SqliteDatabase db, long _id, long number, String name) {
  db.insert(HockeyPlayer.TABLE_NAME, null, HockeyPlayer.FACTORY.marshal()
    ._id(_id)
    .number(number)
    .name(name)
    .asContentValues());
}

public List<HockeyPlayer> alecs(SQLiteDatabase db) {
  List<HockeyPlayer> result = new ArrayList<>();
  try (Cursor cursor = db.rawQuery(HockeyPlayer.SELECT_BY_NAME, new String[] { "Alec" })) {
    while (cursor.moveToNext()) {
      result.add(HockeyPlayer.MAPPER.map(cursor));
    }
  }
  return result;
}
```

4. Projections
>Each select statement will have an interface and mapper generated for it, as well as a method on the factory to create a new instance of the mapper.


5. Types (数据类型)
>SQLDelight column definition are identical to regular SQLite column definitions but support an extra column constraint which specifies the java type of the column in the generated interface. SQLDelight natively supports the same types that Cursor and ContentValues expect:

SQLDelight 的数据列定义和普通的 SQL 列定义一样, 但是在生成的接口中支持一些额外的用于指定列数据类型属性. SQLDelight 直接支持 Cursor 和 ContentValues 需要的数据类型.

```
CREATE TABLE some_types {
  some_long INTEGER,           -- Stored as INTEGER in db, retrieved as Long
  some_double REAL,            -- 以 REAL 类型保存在数据库, 在生成的对象中为 Double
  some_string TEXT,            -- Stored as TEXT in db, retrieved as String
  some_blob BLOB,              -- Stored as BLOB in db, retrieved as byte[]
  some_int INTEGER AS Integer, -- Stored as INTEGER in db, retrieved as Integer
  some_short INTEGER AS Short, -- Stored as INTEGER in db, retrieved as Short
  some_float REAL AS Float     -- Stored as REAL in db, retrieved as Float
}
```

6. Booleans
>SQLDelight supports boolean columns and stores them in the db as ints. Since they are implemented as ints they can be given int column constraints:

 SQLDelight 支持在数据库中以 int 形式存储 boolean 值.

```
CREATE TABLE hockey_player (
  injured INTEGER AS Boolean DEFAULT 0
)
```

7. Custom Classes (自定义类型)
>If you'd like to retrieve columns as custom types you can specify the java type as a sqlite string:

如果你想使用自定义类型修饰列, 你可以直接指定对应的 Java 类型.

```
import java.util.Calendar;

CREATE TABLE hockey_player (
  birth_date INTEGER AS Calendar NOT NULL
)
```

>However, creating a Marshal or Factory will require you to provide a ColumnAdapter which knows how to map between the database type and your custom type:

然而, 使用自定义类型在创建 Marshal 或 Factory 时需要你提供给一个 ColumnAdapter 来展示数据库类型和自定义类型间映射关系 :

```
public class HockeyPlayer implements HockeyPlayerModel {
  private static final ColumnAdapter<Calendar, Long> CALENDAR_ADAPTER = new ColumnAdapter<>() {
    @Override public Calendar map(Long databaseValue) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(databaseValue);
      return calendar;
    }

    @Override public Long encode(Calendar value) {
      return value.getTimeInMillis();
    }
  }

  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(new Creator<>() { },
      CALENDAR_ADAPTER);
}
```

8. Enums (枚举)
>As a convenience the SQLDelight runtime includes a ColumnAdapter for storing an enum as TEXT.

为了方便使用枚举类型, SQLDelight 包含一个 ColumnAdapter 把枚举类型保存为 TEXT.

```
import com.example.hockey.HockeyPlayer;

CREATE TABLE hockey_player (
  position TEXT AS HockeyPlayer.Position
)
public class HockeyPlayer implements HockeyPlayerModel 
{
	public enum Position 
	{
	    CENTER, LEFT_WING, RIGHT_WING, DEFENSE, GOALIE
    }

	private static final ColumnAdapter<Position, String> POSITION_ADAPTER = EnumColumnAdapter.create(Position.class);

	public static final Factory<HockeyPlayer> FACTORY = new Factory<>(new Creator<>() { }, POSITION_ADAPTER);
	
}
```

9. SQL Statement Arguments
>SQL queries can also contain arguments the same way SqliteDatabase does:

SqliteDatabase 中的参数支持 `.sq` 中的语句

```
select_by_position:
SELECT *
FROM hockey_player
WHERE position = ?;

Cursor centers = db.rawQuery(HockeyPlayer.SELECT_BY_POSITION, new String[] { Center.name() });
```

10. Views
>Views receive the same treatment in generated code as tables with their own model interface.

View 在生成的模型接口中可以看做 table.

11. Join Projections
>Selecting from multiple tables via joins also requires an implementation class.

 通过 Join 查询关联表也需要一个实现类

```
select_all_info:
SELECT *
FROM hockey_player
JOIN names USING (_id);
```

生成接口

```
interface HockeyPlayerModel {

  ...

  interface Select_all_infoModel<T1 extends HockeyPlayerModel, V4 extends NamesModel> {
    T1 hockey_player();

    V4 names();
  }

  interface Select_all_infoCreator<T1 extends HockeyPlayerModel, V4 extends NamesModel, T extends Select_all_infoModel<T1, V4>> {
    T create(T1 hockey_player, V4 names);
  }

  final class Select_all_infoMapper<T1 extends HockeyPlayerModel, V4 extends NamesModel, T extends Select_all_infoModel<T1, V4>> implements RowMapper<T> {
    ...
  }

  final class Factory<T extends HockeyPlayerModel> {
    public <V4 extends NamesModel, R extends Select_all_infoModel<T, V4>> Select_all_infoMapper<T, V4, R> select_all_infoMapper(Select_all_infoCreator<T, V4, R> creator, NamesCreator<V4> namesCreator) {
      return new Select_all_infoMapper<T, V4, R>(creator, this, namesCreator);
    }
  }
}
```

实现类

```
AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(AutoValue_HockeyPlayer::new);

  public static final RowMapper<AllInfo> SELECT_ALL_INFO_MAPPER =
      FACTORY.select_all_infoMapper(AutoValue_HockeyPlayer_AllInfo::new,
        AutoValue_HockeyPlayer_Names::new);

  public List<AllInfo> allInfo(SQLiteDatabase db) {
    List<AllInfo> allInfoList = new ArrayList<>();
    try (Cursor cursor = db.rawQuery(SELECT_ALL_INFO)) {
      while (cursor.moveToNext()) {
        allInfoList.add(SELECT_ALL_INFO_MAPPER.map(cursor));
      }
    }
    return allInfoList;
  }

  @AutoValue
  public abstract class Names implements NamesModel { }

  @AutoValue
  public abstract class AllInfo implements Select_all_infoModel<HockeyPlayer, Names> { }
}
```

####关于SqlBrite和SqlDelight的思考

有兴趣的可以看看大神Jack Wharton在国外著名新闻站点Reddit中的回答。这里做出翻译。

SqlBrite和SqlDelight都是对象映射（OM，Object Mappers）而不是对象关系映射（ORM，Object/Relational Mappers）。

ORM 其实并不是一个优秀的框架。很多平台的 ORM 实现都有性能和内存的问题。我们也不会编写ORM。

SqlBrite只是让你方便在 RxJava 中使用 Sql 操作而已，并且额外添加了对数据库表数据更新通知的机制。只是一个 SQLiteOpenHelper 的轻量级封装，并不关心你的对象是如何实现的，也不关心你的数据库。同样，SqlBrite也不支持对象映射和类型安全的查询，通常这些功能并不比直接使用SQL 语句更加方便。虽然在 Java 中操作 SQL 语言有一个比较好的框架 — jOOQ 。但是在 Android 中使用 jOOQ 就是杀鸡用牛刀了！

SqlDelight 的做法是从 SQL 语句来生成 JAVA 模型代码。 这样的好处是，所有 SQL 语句都位于同一个位置，通过查看 SQL 语句可以清楚的了解需要实现的功能和数据库的结构。SqlDelight 添加了对 SQL 语句的编译时验证、表名字和列名字的代码自动完成功能。让编写 SQL 语句更加快捷。在编译的时候，根据 SQL 语句生成 Java 模型接口和 builder 来把数据行和 Java 对象实现转换。虽然这个框架还很年轻，但是通过这个框架目前的功能你就可以发现，SqlDelight 不会变成一个 ORM 框架。并且不会做很重的功能（比如数据懒加载、缓存 、级联删除 等 ORM 框架内常见的功能） 。

SqlDelight 大部分代码都是编译时用的，真正的运行时代码（包含在你应用中的代码）只有10几行代码几个接口而已。它将会使你的SQL编写更加简单，迁移到上面这两个库也会非常的简单，同时你也能享受到响应式的查询，类型安全的对象映射和编译的优点。

这两个框架将不会实现那些ORM框架强制要求你做的事情下面这些功能： 
- 不会成为 Java 语言中功能不够全面的数据库查询 API 
- 不会实现把外键映射为 Java 对象集合（关系映射） 
- 不会有泛字符类型（string-ly typed）的表名字和列名字的引用 
- 不会有一个基类需要你的数据库操作对象来继承该类 
- 不会在 Java 中定义数据库表，比如通过注解、或者继承一个类等 
- 不会自动创建数据表和迁移数据表 
- 不会对 Sql 查询和 Java 对象做线程限制 
- 不会返回可变的对象，你修改该对象的值，然后调用 save 函数就可以把更新的值保存到数据库了。

SqlBrite 仅仅是一个用来协调更新数据和通知数据变化的轻量级封装，当你对数据表进行操作的时候，其他订阅者可以在数据发生变化的时候收到通知。然后可以用 RxJava 的方式来操作数据。 
SqlBrite 不是一个 ORM 框架，也不是一个类型安全的查询框架。不会提供类似Gson中对象序列化的功能，也不会提供数据库迁移的功能。其中的一些功能由可以与SqlBrite一起使用的 SQLDelight提供。

####编码疑难

#####1. 如何在项目中添加SQLDelight依赖

- 在 project 的 build.gradle 中添加:

```
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:0.4.4'
  }
}
```

- 在 Model 的 build.gradle 中添加:

```
apply plugin: 'com.squareup.sqldelight'
```

- Square那帮人还制作了一个支持语法与高亮的IntelliJ插件, 推荐安装, 它可以帮助你避免一些语法错误. 打开 Android Studio -> Preferences -> Plugins -> 搜索与安装 SQLDelight.

#####2. 添加AutoValue 依赖
[使用 Google AutoValue 自动生成代码](http://www.jianshu.com/p/0e2be3536a4e)
[android-apt](http://www.jianshu.com/p/2494825183c5)
```
dependencies {
  provided 'com.google.auto.value:auto-value:1.2'
  apt 'com.google.auto.value:auto-value:1.2'
  apt 'com.ryanharter.auto.value:auto-value-parcel:0.2.1'
}
```

>第三个依赖来实现Parcelable接口.

由于这里使用了 `apt` 需要在 Project 的 build.gradle 中添加:

```
buildscript {
    repositories {
      mavenCentral()
    }
    dependencies {
        //替换成最新的 gradle版本
        classpath 'com.android.tools.build:gradle:1.3.0'
        //替换成最新android-apt版本
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    }
}
```

在 Module 下的 build.gradle 中添加:

```
apply plugin: 'com.neenbedankt.android-apt'
```


[转载自](http://blog.csdn.net/u014315849/article/details/51958088)
