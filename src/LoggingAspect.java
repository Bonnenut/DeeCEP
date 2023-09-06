import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* java.io.PrintStream.println(..))")
    public void printPointcut() {}

    @After("printPointcut()")
    public void log(JoinPoint joinPoint) {

        // 获取 println 方法的参数
        Object[] args = joinPoint.getArgs();

        // 判断参数是否为字符串
        if(args[0] instanceof String) {

            // 取出字符串参数打印到日志
            String message = (String) args[0];
            logger.info(message);

        }

    }

}