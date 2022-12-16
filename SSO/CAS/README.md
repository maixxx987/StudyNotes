# 中心认证服务(Central Authentication Service)

> CAS是**Central Authentication Service**的首字母缩写，Apereo CAS 是由耶鲁大学实验室2002年出的一个开源的统一认证服务。旨在为 Web 应用系统提供一种可靠的单点登录解决方法。
>
> [CAS | Apereo](https://www.apereo.org/projects/cas)



## CAS Client

> 此处说明基于3.2.1版本(内心OS: emm, 这是十年前的版本, 但因为工作上对接的几个客户都是这个版本......) 



### 相关Filter

本段主要是记录下各种常见的filter及其作用。最核心的过滤器为AuthenticationFilter和具体的TicketValidator。

后续的HttpServletRequestWrapperFilter和AssertionThreadLocalFilter可不配置，通过如下方法获取用户名。

```java
Assertion assertion = (Assertion) (request.getSession() == null ? request.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION) : request.getSession().getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION));

if(null != assertion) {
    String userName = assertion.getPrincipal().getName();
}
```





#### org.jasig.cas.client.authentication.AuthenticationFilter

> 核心Filter，检测当前用户是否有登录，若没登录则跳转到casServerLoginUrl 登录

##### 需要配置参数

- casServerLoginUrl: CAS服务器登录页，如 http://casServer.com/login

- serverName: 认证成功后跳转的服务器地址（需包含端口号，具体接口为当前请求的URI，如指定serverName为“http://localhost:8080”，而当前请求的URI为“/app”，查询参数为“a=b&b=c”，则对应认证成功后的跳转地址将为“http://localhost:8080/app?a=b&b=c）

- service: 认证成功后跳转的服务器 **接口** 地址（如http://localhost:8080/sso）

  注：serverName和service只需要指定一个即可，若两个都指定了则采用service



##### 处理流程

1. 判断有无session，session中有无该用户，若有则执行下一个filter
2. 判断连接中是否包含ticket参数，若有则执行下一个filter
3. 重定向到第三方cas认证服务器



#### org.jasig.cas.client.validation.TicketValidator

> 对于client接收到的ticket进行验证

cas中包含了很多TicketValidaor，都继承自AbstractTicketValidationFilter

- Cas10TicketValidationFilter
- Cas20ProxyReceivingTicketValidationFilter 
- Saml11TicketValidationFilter



##### 需要配置参数

- casServerUrlPrefix : CAS服务器的前缀
- serverName或service：与上面相同



##### 处理流程

1. 从request获取ticket参数，如果ticket为空，继续处理下一个过滤器。如果参数不为空，验证ticket参数的合法性。
2. 验证ticket：this.ticketValidator.validate方法通过httpClient访问CAS服务器端验证ticket是否正确，并返回assertion对象。如果验证失败，抛出异常，跳转到错误页面。如果验证成功，session会以"const_cas_assertion"的名称保存assertion对象，继续处理下一个过滤器。



#### org.jasig.cas.client.util.HttpServletRequestWrapperFilter

> 该过滤器负责实现HttpServletRequest请求的包裹， 比如允许开发者通过HttpServletRequest的getRemoteUser()方法获得SSO登录用户的登录名



#### org.jasig.cas.client.util.AssertionThreadLocalFilter

> 该过滤器使得开发者可以通过org.jasig.cas.client.util.AssertionHolder来获取用户的登录名。 比如AssertionHolder.getAssertion().getPrincipal().getName()



#### org.jasig.cas.client.session.SingleSignOutFilter

> 该过滤器用于实现单点登出功能



### WEB.xml 配置

```xml
<!-- CAS 单点登录(SSO) 过滤器配置 (start) -->  
      
    <!-- 该过滤器用于实现单点登出功能。-->  
    <filter>  
        <filter-name>CAS Single Sign Out Filter</filter-name>  
        <filter-class>org.jasig.cas.client.session.SingleSignOutFilter</filter-class>  
    </filter>  
    <filter-mapping>  
        <filter-name>CAS Single Sign Out Filter</filter-name>  
        <url-pattern>/*</url-pattern>  
    </filter-mapping>  
    <!-- CAS: 用于单点退出 -->  
    <listener>  
        <listener-class>org.jasig.cas.client.session.SingleSignOutHttpSessionListener</listener-class>  
    </listener>  
      
    <!-- 该过滤器负责用户的认证工作，必须启用它 -->  
    <filter>  
        <filter-name>CASFilter</filter-name>  
        <filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>  
        <init-param>  
            <param-name>casServerLoginUrl</param-name>  
            <!-- 下面的URL是Cas服务器的登录地址 -->  
            <param-value>http://CAS服务端所在服务器IP:8080/cas/login</param-value>  
        </init-param>  
        <init-param>  
            <param-name>serverName</param-name>  
            <!-- 下面的URL当前应用的访问地址 -->  
            <param-value>http://具体web应用程序所在服务器IP:8080</param-value>  
        </init-param>  
    </filter>  
    <filter-mapping>  
        <filter-name>CASFilter</filter-name>  
        <url-pattern>/*</url-pattern>  
    </filter-mapping>  
       
    <!-- 该过滤器负责对Ticket的校验工作，必须启用它 -->  
    <filter>  
        <filter-name>CAS Validation Filter</filter-name>  
        <filter-class>org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter</filter-class>  
        <init-param>  
            <param-name>casServerUrlPrefix</param-name>  
            <!-- 下面的URL是Cas服务器的认证地址 -->  
            <param-value>http://CAS服务端所在服务器IP:8080/cas</param-value>  
        </init-param>  
        <init-param>  
            <param-name>serverName</param-name>  
            <!-- 下面的URL是具体某一个应用的访问地址 -->  
            <param-value>http://具体web应用程序所在服务器IP:8080</param-value>  
        </init-param>  
        <init-param>  
          <param-name>renew</param-name>  
          <param-value>false</param-value>  
        </init-param>  
        <init-param>  
          <param-name>gateway</param-name>  
          <param-value>false</param-value>  
        </init-param>  
    </filter>  
    <filter-mapping>  
        <filter-name>CAS Validation Filter</filter-name>  
        <url-pattern>/*</url-pattern>  
    </filter-mapping>  
       
    <!--  
    该过滤器负责实现HttpServletRequest请求的包裹，  
    比如允许开发者通过HttpServletRequest的getRemoteUser()方法获得SSO登录用户的登录名，可选配置。  
    -->  
    <filter>  
        <filter-name>CAS HttpServletRequest Wrapper Filter</filter-name>  
        <filter-class>org.jasig.cas.client.util.HttpServletRequestWrapperFilter</filter-class>  
    </filter>  
    <filter-mapping>  
        <filter-name>CAS HttpServletRequest Wrapper Filter</filter-name>  
        <url-pattern>/*</url-pattern>  
    </filter-mapping>  
       
    <!--  
    该过滤器使得开发者可以通过org.jasig.cas.client.util.AssertionHolder来获取用户的登录名。  
    比如AssertionHolder.getAssertion().getPrincipal().getName()。  
    -->  
    <filter>  
        <filter-name>CAS Assertion Thread Local Filter</filter-name>  
        <filter-class>org.jasig.cas.client.util.AssertionThreadLocalFilter</filter-class>  
    </filter>  
    <filter-mapping>  
        <filter-name>CAS Assertion Thread Local Filter</filter-name>  
        <url-pattern>/*</url-pattern>  
    </filter-mapping>  
       
    <!-- 自动根据单点登录的结果设置本系统的用户信息（具体某一个应用实现） -->  
    <filter>  
        <filter-name>CasForInvokeContextFilter</filter-name>  
        <filter-class>com.cm.demo.filter.CasForInvokeContextFilter</filter-class>  
        <init-param>  
          <param-name>appId</param-name>  
          <param-value>a5ea611bbff7474a81753697a1714fb0</param-value>  
        </init-param>  
    </filter>  
    <filter-mapping>  
        <filter-name>CasForInvokeContextFilter</filter-name>  
        <url-pattern>/*</url-pattern>  
    </filter-mapping>  
    <!-- CAS 单点登录(SSO) 过滤器配置 (end) -->
```





### SpringBoot 配置

> SpringBoot实现过滤器有两种实现方式，一种是基于注解，另一种是基于过滤器配置Bean
>
> 因为此处有较多个过滤器需要配置，且不需要实现里面的具体代码，因此更推荐第二种方式



#### WebFilter注解

``` java
@WebFilter(filterName = "CASAuthenticationFilter", urlPatterns = "/*", initParams = {
        @WebInitParam(name = "casServerLoginUrl", value = "https://cas.cn/login"),
        @WebInitParam(name = "serverName", value = "http://localhost:8080")
})
//自定义一个继承CAS过滤器的过滤器，不用具体实现也可以生效
public class CasAuthenticationFilter extends AuthenticationFilter {
}
```



#### 基于FilterRegistrationBean

``` java
@Configuration
public class CasConfig {
 
    // CAS登录地址
    @Value("${cas.casServerLoginUrl}")
    private String casServerLoginUrl;
    
    // 本地服务器地址(需要包含端口号)
    @Value("${cas.serverName}")
    private String serverName;
    
    // CAS服务器地址前缀
    @Value("${cas.casServerUrlPrefix}")
    private String casServerUrlPrefix;


    /**
     * CASFilter
     * org.jasig.cas.client.authentication.AuthenticationFilter
     * 判断有没有登录，没有登录则跳转到登录页面
     */
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> casFilter() {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthenticationFilter());
        registration.addUrlPatterns("/*");
        registration.addInitParameter("casServerLoginUrl", casServerLoginUrl);
        registration.addInitParameter("serverName", serverName);
        return registration;
    }

    /**
     * Cas20ProxyReceivingTicketValidationFilter
     * 校验对方回传的tick的过滤器
     */
    @Bean
    public FilterRegistrationBean<Cas20ProxyReceivingTicketValidationFilter> casValidationFilter() {
        FilterRegistrationBean<Cas20ProxyReceivingTicketValidationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Cas20ProxyReceivingTicketValidationFilter());
        registration.addUrlPatterns("/*");
        registration.addInitParameter("casServerUrlPrefix", casServerUrlPrefix);
        registration.addInitParameter("serverName", serverName);
        return registration;
    }

    /**
     * HttpServletRequestWrapperFilter
     * 可选配置, 配置后可通过HttpServletRequest.getRemoteUser()方法获得CAS服务器中的用户名
     */
    @Bean
    public FilterRegistrationBean<HttpServletRequestWrapperFilter> casHttpServletRequestWrapperFilter() {
        FilterRegistrationBean<HttpServletRequestWrapperFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpServletRequestWrapperFilter());
        return registration;
    }

    /**
     * AssertionThreadLocalFilter
     * 可选配置, 配置后可通过AssertionHolder.getAssertion().getPrincipal().getName()来获取用户名
     */
    @Bean
    public FilterRegistrationBean<AssertionThreadLocalFilter> casAssertionThreadLocalFilter() {
        FilterRegistrationBean<AssertionThreadLocalFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AssertionThreadLocalFilter());
        return registration;
    }

    /**
     * 登出Filter
     */
    @Bean
    public FilterRegistrationBean<SingleSignOutFilter> casSingleSignOutFilter() {
        FilterRegistrationBean<SingleSignOutFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SingleSignOutFilter());
        registration.addUrlPatterns("/logout");
        return registration;
    }
}
```



### 遇到的问题及解决

#### 无法继承重写Filter内的doFilter方法

> 如果有部分需求，需要绕过CAS校验，通常情况下会继承对应的Filter，然后重写doFilter方法。
>
> 但由于CAS的doFilter方法采用了final修饰，因此无法继承重写。
>
> 此处提供三个方法供参考，其中前两个方法已验证可行。

##### 自行管理CAS Filter

- 自定定义Filter，内部声明CAS Filter变量

- 自定义Filter执行init的时候，初始化CAS Filter并init

- 自定义Filter执行doFilter方法时，添加自己的需求逻辑，判断是否要绕过CAS Filter
  - 如果不需要，则直接调用chain.doFilter
  - 如果需要，则调用CAS Filter的doFilter
- 自定义Filter执行destroy的时候，调用CAS Filter的destroy



相关配置

```java
    @Bean
    public FilterRegistrationBean<BeforeAuthenticationFilter> beforeAuthenticationFilter() {
        FilterRegistrationBean<BeforeAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new BeforeAuthenticationFilter());
        registration.addUrlPatterns("/*");
        registration.addInitParameter("casServerLoginUrl", casServerLoginUrl);
        registration.addInitParameter("serverName", serverName);
        return registration;
    }
```



自定义Filter具体代码

```java
public class BeforeAuthenticationFilter implements Filter {
    private AuthenticationFilter authenticationFilter;
    private static final Logger LOGGER = LoggerFactory.getLogger(BeforeAuthenticationFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.info("[BeforeAuthenticationFilter#init] init");
        // 初始化authenticationFilter
        authenticationFilter = new AuthenticationFilter();
        // 此处的参数在FilterBean中配置
        authenticationFilter.setCasServerLoginUrl(filterConfig.getInitParameter("casServerLoginUrl"));
        authenticationFilter.setServerName(filterConfig.getInitParameter("serverName"));
        authenticationFilter.init();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 执行自己的判断逻辑，判断是否过滤掉CAS Filter
        String id = ((HttpServletRequest) request).getHeader("id");
        if (StringUtils.isNotEmpty(identifierName) && id.equals("999")) {
            LOGGER.info("[BeforeAuthenticationFilter#doFilter] id:{}, 执行AuthenticationFilter", identifierName);
            authenticationFilter.doFilter(request, response, chain);
        } else {
            LOGGER.info("[BeforeAuthenticationFilter#doFilter] id:{}, 跳过AuthenticationFilter", identifierName);
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("[BeforeAuthenticationFilter#destroy] destroy");
        authenticationFilter.destroy();
    }
}
```



##### 将CAS Filter从 FilterChain 中移除

> 和自行管理Filter大同小异，只在doFilter中有差距，因此此处仅贴出doFilter的代码

由于Servlet的Filter是一个链表，因此在执行CAS Filter前，可以手动将后续的CAS Filter删除，达到不经过CAS Filter的效果

具体代码

```java

/**
 * 需要跳过的cas filter
 */
private static final List<String> CAS_FILTERS = Stream.of("org.jasig.cas.client.authentication.AuthenticationFilter",
        "org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter",
        "org.jasig.cas.client.util.HttpServletRequestWrapperFilter",
        "org.jasig.cas.client.util.AssertionThreadLocalFilter",
        "org.jasig.cas.client.session.SingleSignOutFilter")
        .collect(Collectors.toList());

@Override
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    // 执行自己的判断逻辑，判断是否过滤掉CAS Filter
    String id = ((HttpServletRequest) request).getHeader("id");
    if (StringUtils.isNotEmpty(identifierName) && id.equals("999")) {
        LOGGER.info("[BeforeCasFilter#doFilter] id:{}, 执行cas", id);
    } else {
        // 删除filter
        LOGGER.info("[BeforeCasFilter#doFilter] 租户id:{}, 跳过cas", TTL.getInstance().get());
        try {
            // 获取过滤器链
            Field filters = chain.getClass().getDeclaredField("filters");
            filters.setAccessible(true);
            FilterConfig[] filterConfigs = (FilterConfig[]) filters.get(chain);
            // 删除的过滤器数量
            int deleteFilterCount = 0;
            // 遍历过滤器链
            for (int i = 0; i < filterConfigs.length; i++) {
                if (filterConfigs[i] != null) {
                    // 获取过滤器信息
                    Field filterDefField = filterConfigs[i].getClass().getDeclaredField("filterDef");
                    filterDefField.setAccessible(true);
                    FilterDef filterDef = (FilterDef) filterDefField.get(filterConfigs[i]);
                    // 获取过滤器名
                    String filterClass = filterDef.getFilterClass();
                    if (CAS_FILTERS.contains(filterClass)) {
                        // 如果是要CAS的过滤器, 则置为空, 同时记录置空的数量
                        filterConfigs[i] = null;
                        deleteFilterCount++;
                    }
                    filterDefField.setAccessible(false);
                }
            }

            // 上次遍历不为空的位置
            int preNext = 0;
            // 遍历过滤器列表, 将中间的空位补全
            for (int i = 0; i < filterConfigs.length; i++) {
                if (null == filterConfigs[i]) {
                    int next = preNext == 0 ? i + 1 : preNext;
                    // 查找下一个不为空的元素
                    while (next < filterConfigs.length && null == filterConfigs[next]) {
                        next++;
                    }
                    if (next >= filterConfigs.length) {
                        // 遍历到末尾了, 直接跳出
                        break;
                    }
                    if (null != filterConfigs[next]) {
                        // 将后续的不为空的元素替换当前为空的元素
                        filterConfigs[i] = filterConfigs[next];
                        // 替换后将后续元素置为空
                        filterConfigs[next] = null;
                        // 记录上次的位置
                        preNext = next + 1;
                    }
                }
            }
            filters.setAccessible(false);

            // 修改过滤器链中的过滤器数量
            Field filterCountField = chain.getClass().getDeclaredField("n");
            filterCountField.setAccessible(true);
            filterCountField.set(chain, filterCountField.getInt(chain) - deleteFilterCount);
            filterCountField.setAccessible(false);
        } catch (Exception e) {
            LOGGER.error("[BeforeCasFilter#doFilter]修改过滤器链异常:{}", e.getMessage(), e);
        }
    }
    chain.doFilter(request, response);
}
```



##### 采用AspectJ进行前置处理
> AspectJ是一个AOP框架，与Spring AOP的区别是，AspectJ在使用独立的编译器，在代码编译期间就实现静态代理。

CasFilterAsepct.aj
```java

public aspect CasFilterAsepct {
    //   环绕通知  
    void around() : execution(* SuperClass.doSomething()) {
        if (thisJoinPoint.getThis() instanceof SubClass) {
            //调用子类方法
            ((SubClass)thisJoinPoint.getThis()).overrideDoSomething();
        } else {
            //调用原方法
            proceed();
        }
    }

```



## 参考链接
- [CAS-Client客户端研究(一)-AuthenticationFilter_yuwenruli的博客-CSDN博客](https://blog.csdn.net/yuwenruli/article/details/6600032)
- [五十二、SpringBoot配置Filter以及注解配置CAS客户端过滤器_仰望星空的尘埃的博客-CSDN博客_@webfilter 配置cas](https://blog.csdn.net/u010285974/article/details/85335005)
- [单点登录--CAS认证--web.xml配置详解 - 一步一个小脚印 - 博客园 (cnblogs.com)](https://www.cnblogs.com/tjudzj/p/10187626.html)
- [CAS之单点登录client逻辑详解_鱼儿塘的博客-CSDN博客](https://blog.csdn.net/m0_47495420/article/details/109760008)
- [Java中final修饰的方法是否可以被重写？_Java编程_yyds的博客-CSDN博客_final类如何重写方法](https://blog.csdn.net/DDDYSz/article/details/109507822)
