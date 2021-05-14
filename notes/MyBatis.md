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

## 动态SQL

[mybatis – MyBatis 3 | 动态 SQL](https://mybatis.org/mybatis-3/zh/dynamic-sql.html)

语法标准是OGNL语法。[OGNL - Apache Commons OGNL - Language Guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)

在转移的时候需要使用`ISO-8859-1`标准。[HTML ISO-8859-1 Reference (w3schools.com)](https://www.w3schools.com/charsets/ref_html_8859.asp)

### IF

````xml
 <select id="getEmpsByConditionIf" resultType="com.atguigu.mybatis.bean.Employee">
    select * from tbl_employee
    <!-- where -->
    <where>
        <!-- test：判断表达式（OGNL）
        OGNL参照PPT或者官方文档。
             c:if  test
        从参数中取值进行判断

        遇见特殊符号应该去写转义字符：
        &&：
        -->
        <if test="id!=null">
            id=#{id}
        </if>
        <if test="lastName!=null &amp;&amp; lastName!=&quot;&quot;">
            and last_name like #{lastName}
        </if>
        <if test="email!=null and email.trim()!=&quot;&quot;">
            and email=#{email}
        </if> 
        <!-- ognl会进行字符串与数字的转换判断  "0"==0 -->
        <if test="gender==0 or gender==1">
            and gender=#{gender}
        </if>
    </where>
 </select>
````

### Switch

用于替代`if-elseif`语句。

```xml
 <!-- public List<Employee> getEmpsByConditionChoose(Employee employee); -->
 <select id="getEmpsByConditionChoose" resultType="com.atguigu.mybatis.bean.Employee">
    select * from tbl_employee 
    <where>
        <!-- 如果带了id就用id查，如果带了lastName就用lastName查;只会进入其中一个 -->
        <choose>
            <when test="id!=null">
                id=#{id}
            </when>
            <when test="lastName!=null">
                last_name like #{lastName}
            </when>
            <when test="email!=null">
                email = #{email}
            </when>
            <otherwise>
                gender = 0
            </otherwise>
        </choose>
    </where>
 </select>
```

### foreach

```xml
<!--public List<Employee> getEmpsByConditionForeach(List<Integer> ids);  -->
<select id="getEmpsByConditionForeach" resultType="com.atguigu.mybatis.bean.Employee">
select * from tbl_employee
<!--
    collection：指定要遍历的集合：
        list类型的参数会特殊处理封装在map中，map的key就叫list
    item：将当前遍历出的元素赋值给指定的变量
    separator:每个元素之间的分隔符
    open：遍历出所有结果拼接一个开始的字符
    close:遍历出所有结果拼接一个结束的字符
    index:索引。遍历list的时候是index就是索引，item就是当前值
                  遍历map的时候index表示的就是map的key，item就是map的值

    #{变量名}就能取出变量的值也就是当前遍历出的元素
  -->
<foreach collection="ids" item="item_id" separator=","
    open="where id in(" close=")">
    #{item_id}
</foreach>
</select>
```

## 缓存机制

````java
/**
 * 两级缓存：
 * 一级缓存：（本地缓存）：sqlSession级别的缓存。一级缓存是一直开启的；SqlSession级别的一个Map
 * 		与数据库同一次会话期间查询到的数据会放在本地缓存中。
 * 		以后如果需要获取相同的数据，直接从缓存中拿，没必要再去查询数据库；
 * 
 * 		一级缓存失效情况（没有使用到当前一级缓存的情况，效果就是，还需要再向数据库发出查询）：
 * 		1、sqlSession不同。
 * 		2、sqlSession相同，查询条件不同.(当前一级缓存中还没有这个数据)
 * 		3、sqlSession相同，两次查询之间执行了增删改操作(这次增删改可能对当前数据有影响)
 * 		4、sqlSession相同，手动清除了一级缓存（缓存清空）
 * 
 * 二级缓存：（全局缓存）：基于namespace级别的缓存：一个namespace对应一个二级缓存：
 * 		工作机制：
 * 		1、一个会话，查询一条数据，这个数据就会被放在当前会话的一级缓存中；
 * 		2、如果会话关闭；一级缓存中的数据会被保存到二级缓存中；新的会话查询信息，就可以参照二级缓存中的内容；
 * 		3、sqlSession===EmployeeMapper==>Employee
 * 						DepartmentMapper===>Department
 * 			不同namespace查出的数据会放在自己对应的缓存中（map）
 * 			效果：数据会从二级缓存中获取
 * 				查出的数据都会被默认先放在一级缓存中。
 * 				只有会话提交或者关闭以后，一级缓存中的数据才会转移到二级缓存中
 * 		使用：
 * 			1）、开启全局二级缓存配置：<setting name="cacheEnabled" value="true"/>
 * 			2）、去mapper.xml中配置使用二级缓存：
 * 				<cache></cache>
 * 			3）、我们的POJO需要实现序列化接口
 * 	
 * 和缓存有关的设置/属性：
 * 			1）、cacheEnabled=true：false：关闭缓存（二级缓存关闭）(一级缓存一直可用的)
 * 			2）、每个select标签都有useCache="true"：
 * 					false：不使用缓存（一级缓存依然使用，二级缓存不使用）
 * 			3）、【每个增删改标签的：flushCache="true"：（一级二级都会清除）】
 * 					增删改执行完成后就会清楚缓存；
 * 					测试：flushCache="true"：一级缓存就清空了；二级也会被清除；
 * 					查询标签：flushCache="false"：
 * 						如果flushCache=true;每次查询之后都会清空缓存；缓存是没有被使用的；
 * 			4）、sqlSession.clearCache();只是清楚当前session的一级缓存；
 * 			5）、localCacheScope：本地缓存作用域：（一级缓存SESSION）；当前会话的所有数据保存在会话缓存中；
 * 								STATEMENT：可以禁用一级缓存；		
 * 				
 *第三方缓存整合：
 *		1）、导入第三方缓存包即可；
 *		2）、导入与第三方缓存整合的适配包；官方有；
 *		3）、mapper.xml中使用自定义缓存
 *		<cache type="org.mybatis.caches.ehcache.EhcacheCache"></cache>
 *
 * @throws IOException 
 * 
 */
````

