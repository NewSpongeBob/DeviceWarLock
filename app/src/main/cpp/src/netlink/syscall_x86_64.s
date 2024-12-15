.text
.global raw_syscall
.type raw_syscall,@function

raw_syscall:
    mov     %rdi, %rax    # 系统调用号移到rax
    mov     %rsi, %rdi    # 第一个参数
    mov     %rdx, %rsi    # 第二个参数
    mov     %rcx, %rdx    # 第三个参数
    mov     %r8,  %r10    # 第四个参数
    mov     %r9,  %r8     # 第五个参数
    mov     8(%rsp), %r9  # 第六个参数
    syscall              # 执行系统调用
    ret