# MyBatis

这个框架上手比较简单，只选择部分需要记录的东西。

## Hello World

`config.xml`负责配置mybatis数据源，全局设置等等。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
<!--    全局设置，此处设置驼峰命名和数据库标准命名的映射-->
    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>
<!--    环境——数据源配置-->
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/tx?characterEncoding=utf8"/>
                <property name="username" value="root"/>
                <property name="password" value="86915"/>
            </dataSource>
        </environment>
    </environments>
<!--    mappers映射，每个数据表对应一个mapper，负责映射接口——sql语句，-->
    <mappers>
        <mapper resource="mapper/BookMapper.xml"/>
    </mappers>
</configuration>
```

`BookMapper.xml`

所谓的mapper，实际上多数指的是`dao`到`sql`的映射，这和spring是一样的，从context中获得配置文件注入的mapper，利用该mapper实现对数据库的操作。那么和配置文件对应的，需要定义一个`mapper.xml`对应的`mapper`接口

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.learning.mybatis.mapper.BookMapper">
    <select id="selectBook" resultType="com.learning.mybatis.bean.Book">
        select isbn, book_name bookName, price from book where isbn = #{isbn}
    </select>

    <insert id="insertBook" parameterType="com.learning.mybatis.bean.Book">
        insert into book values (#{isbn}, #{bookName},#{price})
    </insert>

    <update id="updateBook" parameterType="com.learning.mybatis.bean.Book">
        update book set price = #{price} where isbn = #{isbn}
    </update>

    <delete id="deleteBook" >
        delete from book where isbn = #{isbn}
    </delete>
</mapper>
```

`mapper`接口

```java
import com.learning.mybatis.bean.Book;

public interface BookMapper {
    Book selectBook(String isbn);

    boolean insertBook(Book book);

    boolean updateBook(Book book);

    boolean deleteBook(String isbn);
}
```

`POJO`

````JAVA
@AllArgsConstructor
@Data
@ToString
@NoArgsConstructor
public class Book {
    private String isbn;
    private String bookName;
    private double price;
}
````

使用方法也非常简单。

````java
public class TEST {
    private SqlSessionFactory sqlSessionFactory;
    @Before
    public void init() throws IOException {
        // 固定写法
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Test
    public void hello() {
        try(SqlSession sqlSession = sqlSessionFactory.openSession()){
            System.out.println(sqlSession.selectOne("selectBook", "ISBN-001").toString());
        }
    }

    @Test
    public void testItf() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            BookMapper mapper = sqlSession.getMapper(BookMapper.class);
            System.out.println(mapper.selectBook("ISBN-001")
            );
        }
    }

    @Test
    public void testInsert() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            BookMapper mapper = sqlSession.getMapper(BookMapper.class);
            System.out.println(mapper.insertBook(new Book("ISBN-053", "B2", 586)));
//            sqlSession.commit();
        }
    }

    @Test
    public void testUpdate() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            BookMapper mapper = sqlSession.getMapper(BookMapper.class);
//            System.out.println(mapper.updateBook(new Book("ISBN-055", null, 888)));
            System.out.println(mapper.deleteBook("ISBN-055"));
            sqlSession.commit();
        }
    }
}
````

## xml配置

ref： [mybatis – MyBatis 3 | 配置](https://mybatis.org/mybatis-3/zh/configuration.html)

- configuration（配置）
  - [properties（属性）](https://mybatis.org/mybatis-3/zh/configuration.html#properties) 例如配置JDBC配置
  - [settings（设置）](https://mybatis.org/mybatis-3/zh/configuration.html#settings) 最重要的配置，全局配置开关，可以配置命名自动映射等
  - [typeAliases（类型别名）](https://mybatis.org/mybatis-3/zh/configuration.html#typeAliases)
  - [typeHandlers（类型处理器）](https://mybatis.org/mybatis-3/zh/configuration.html#typeHandlers)
  - [objectFactory（对象工厂）](https://mybatis.org/mybatis-3/zh/configuration.html#objectFactory)
  - [plugins（插件）](https://mybatis.org/mybatis-3/zh/configuration.html#plugins)
  - environments（环境配置）数据库环境，可以有多个
    - environment（环境变量）
      - transactionManager（事务管理器）
      - dataSource（数据源）
  - [databaseIdProvider（数据库厂商标识）](https://mybatis.org/mybatis-3/zh/configuration.html#databaseIdProvider)
  - [mappers（映射器）](https://mybatis.org/mybatis-3/zh/configuration.html#mappers)

## 参数传递

- 单个参数，基本类型，对象类型，集合类型
- 多个参数
  - 会被MyBatis重新包装成一个Map传入。Map的key是param1，param2，0，1…，值就是参数的值。
  - 命名参数，使用`@Param`命名参数
  - POJO
  - Map封装

###  源码解析

```java
public ParamNameResolver(Configuration config, Method method) {
    this.useActualParamName = config.isUseActualParamName();
    // 反射获得类型
    final Class<?>[] paramTypes = method.getParameterTypes();
    // 反射获得注解
    final Annotation[][] paramAnnotations = method.getParameterAnnotations();
    final SortedMap<Integer, String> map = new TreeMap<>();
    int paramCount = paramAnnotations.length;
    // 遍历所有参数，封装为map：names
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      // 特殊参数？
      if (isSpecialParameter(paramTypes[paramIndex])) {
        // skip special parameters
        continue;
      }
      // 如果有注解，则用注解的命名作为key
      String name = null;
      for (Annotation annotation : paramAnnotations[paramIndex]) {
        if (annotation instanceof Param) {
          hasParamAnnotation = true;
          name = ((Param) annotation).value();
          break;
        }
      }
      // 如果name为空，则使用param + index作为名字
      if (name == null) {
        // @Param was not specified.
        if (useActualParamName) {
          name = getActualParamName(method, paramIndex);
        }
        if (name == null) {
          // use the parameter index as the name ("0", "1", ...)
          // gcode issue #71
          name = String.valueOf(map.size());
        }
      }
      map.put(paramIndex, name);
    }
    names = Collections.unmodifiableSortedMap(map);
}
```

解析参数，建立name -> param的映射。

```java
public Object getNamedParams(Object[] args) {
    final int paramCount = names.size();
    if (args == null || paramCount == 0) {
      return null;
    } else if (!hasParamAnnotation && paramCount == 1) {
      Object value = args[names.firstKey()];
      return wrapToMapIfCollection(value, useActualParamName ? names.get(0) : null);
    } else {
      final Map<String, Object> param = new ParamMap<>();
      int i = 0;
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        param.put(entry.getValue(), args[entry.getKey()]);
        // add generic param names (param1, param2, ...)
        final String genericParamName = GENERIC_NAME_PREFIX + (i + 1);
        // ensure not to overwrite parameter named with @Param
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      return param;
    }
}
```

## 级联查询

级联查询是关系数据库的基础功能，但是在我印象中，框架对这部分的解决很僵硬。我觉得本质上的原因是很多框架封装过度或者像JDBC那样完全灵活，导致结果很难使用。因为外键这个东西普遍存在，所以这个功能非常重要。

来看看MyBatis提供的解决方案，使用returnMap自定义封装。

````xml
<!--级联查询，自定义规则map-->
<resultMap id="map1" type="com.learning.mybatis.bean2.Book">
    <result column="isbn" property="isbn"/>
    <result column="book_name" property="bookName"/>
    <result column="bs_isbn" property="bookStock.isbn"/>
    <result column="stock" property="bookStock.stock"/>
</resultMap>
<!--级联查询，自定义规则map二, association属性-->
<resultMap id="map2" type="com.learning.mybatis.bean2.Book">
    <result column="isbn" property="isbn"/>
    <result column="book_name" property="bookName"/>
    <association property="bookStock" javaType="com.learning.mybatis.bean2.BookStock">
        <result column="bs_isbn" property="isbn"/>
        <result column="stock" property="stock"/>
    </association>
</resultMap>

<select id="selectBookAndStock" resultMap="map2">
    select book.isbn isbn, book_name, price, book_stock.isbn bs_isbn, stock
    from book, book_stock
    where book.isbn =  book_stock.isbn and book.isbn = #{isbn}
</select>
````

它还支持二级查询，即先查询一个表，然后用另一个表的mapper去查询需要的数据。

```xml
<!--级联查询，自定义规则map二, association属性-->
<resultMap id="map3" type="com.learning.mybatis.bean2.Book">
    <result column="isbn" property="isbn"/>
    <result column="book_name" property="bookName"/>
    // 注意，这里引用了另一个表的mapper
    <association property="bookStock"      select="com.learning.mybatis.mapper.BookStockMapper.selectBookStock"
                 column="isbn">
    </association>
</resultMap>

<select id="selectBookStep" resultMap="map3">
    select * from book where isbn = #{isbn}
</select>
```

更妙的是，二级查询可以开启懒加载。

````xml
<!--    全局设置，此处设置驼峰命名和数据库标准命名的映射-->
<settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
    <!--        开启懒加载-->
    <setting name="lazyLoadingEnabled" value="true"/>
    <setting name="aggressiveLazyLoading" value="false"/>
</settings>
````

