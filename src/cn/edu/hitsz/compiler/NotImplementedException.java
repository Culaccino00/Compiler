package cn.edu.hitsz.compiler;

/**
 * 当一个函数需要你实现, 而你没有实现它的时候, 便会抛出此异常
 */
public class NotImplementedException extends RuntimeException {
}
//python -u ./scripts/check-result.py n ./data/std ./data/out