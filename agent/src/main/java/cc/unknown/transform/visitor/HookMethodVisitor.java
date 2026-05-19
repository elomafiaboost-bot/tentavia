package cc.unknown.transform.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Wraps a MethodVisitor to inject bytecode at method entry and/or before each return.
 * The Injector receives the delegate MethodVisitor so it can emit any sequence of instructions,
 * including jumps and labels (required for cancellable hooks).
 */
public class HookMethodVisitor extends MethodVisitor implements Opcodes {

    public interface Injector {
        void inject(MethodVisitor mv);
    }

    private final Injector pre;
    private final Injector post;

    public HookMethodVisitor(MethodVisitor mv, Injector pre, Injector post) {
        super(ASM5, mv);
        this.pre = pre;
        this.post = post;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (pre != null) pre.inject(mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (post != null && opcode >= IRETURN && opcode <= RETURN) {
            post.inject(mv);
        }
        super.visitInsn(opcode);
    }
}
