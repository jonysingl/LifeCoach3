// 当前用户状态和对话状态
let currentUser = null;
let currentConversationId = null;
let currentMessages = [];

// 格式化日期函数
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 显示通知函数
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);

    // 3秒后自动消失
    setTimeout(() => {
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// 页面加载时检查登录状态
document.addEventListener('DOMContentLoaded', function() {
    const loginButton = document.getElementById('loginButton');
    const loginText = document.getElementById('loginText');

    // 检查用户登录状态
    fetch('/api/check-login')
        .then(response => response.json())
        .then(data => {
            if (data.loggedIn) {
                // 已登录状态
                currentUser = {
                    username: data.username
                };

                // 修改登录按钮为用户信息及登出选项
                loginButton.innerHTML = `
                    <div class="avatar">
                        ${data.username.charAt(0).toUpperCase()}
                    </div>
                    <span>${data.username}</span>
                    <span class="logout-link" style="margin-left: 8px; font-size: 12px; cursor: pointer; text-decoration: underline;">登出</span>
                `;

                // 添加登出功能
                const logoutLink = loginButton.querySelector('.logout-link');
                logoutLink.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    logout();
                });

                // 移除登录按钮的点击重定向
                loginButton.removeAttribute('href');

                // 加载对话历史列表
                loadConversations();
            }
        })
        .catch(error => {
            console.error('检查登录状态失败:', error);
        });

    // 初始状态显示欢迎消息
    showWelcomeMessage();
});

// 显示欢迎消息
function showWelcomeMessage() {
    const chatOutput = document.getElementById('chatOutput');
    chatOutput.innerHTML = '';

    var welcomeMessage = document.createElement('div');
    welcomeMessage.className = 'response-message';
    welcomeMessage.textContent = "您好！我是智能聊天助手，有什么可以帮您的吗？";
    addTimestamp(welcomeMessage);
    chatOutput.appendChild(welcomeMessage);
}

// 登出函数
function logout() {
    fetch('/logout')
        .then(() => {
            // 重新加载页面
            window.location.reload();
        })
        .catch(error => {
            console.error('登出错误:', error);
        });
}

// 加载对话列表
function loadConversations() {
    if (!currentUser) return;

    fetch('/api/conversations')
        .then(response => {
            if (!response.ok) {
                throw new Error('获取对话列表失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                const historyContainer = document.getElementById('conversationHistory');
                // 保留标题，清除其他内容
                const title = historyContainer.querySelector('.history-title');
                historyContainer.innerHTML = '';
                historyContainer.appendChild(title);

                if (data.conversations && data.conversations.length > 0) {
                    // 添加所有对话历史
                    data.conversations.forEach(conv => {
                        const convItem = document.createElement('div');
                        convItem.className = 'conversation-item';
                        convItem.dataset.id = conv.conversation_id;

                        // 使用对话标题，如果没有就使用第一条消息的内容作为预览
                        const preview = conv.first_message ?
                            (conv.first_message.length > 20 ?
                                conv.first_message.substring(0, 20) + '...' :
                                conv.first_message) :
                            '空对话';

                        convItem.innerHTML = `
                            <div class="conversation-title">${conv.title || '无标题对话'}</div>
                            <div class="conversation-menu-trigger">
                                <i class="fa fa-ellipsis-v"></i>
                            </div>
                            <div class="conversation-preview">${preview}</div>
                            <div class="conversation-date">${formatDate(conv.updated_at)}</div>
                            <div class="conversation-menu">
                                <div class="menu-item rename-conversation">重命名</div>
                                <div class="menu-item delete-conversation">删除</div>
                            </div>
                        `;

                        // 点击加载对话内容
                        convItem.addEventListener('click', function(e) {
                            // 忽略点击菜单触发器和菜单项时的事件
                            if (e.target.closest('.conversation-menu-trigger') || e.target.closest('.conversation-menu')) {
                                return;
                            }

                            // 移除其他项目的active类
                            const allItems = historyContainer.querySelectorAll('.conversation-item');
                            allItems.forEach(item => item.classList.remove('active'));

                            // 为当前项目添加active类
                            this.classList.add('active');

                            // 加载此对话内容
                            loadConversationMessages(conv.conversation_id);
                        });

                        historyContainer.appendChild(convItem);
                    });

                    // 设置对话菜单事件
                    setupConversationMenus();
                } else {
                    // 没有对话记录
                    const emptyNotice = document.createElement('div');
                    emptyNotice.className = 'conversation-item';
                    emptyNotice.textContent = '暂无对话记录';
                    historyContainer.appendChild(emptyNotice);
                }
            }
        })
        .catch(error => {
            console.error('加载对话列表失败:', error);
        });
}

// 设置对话菜单事件
function setupConversationMenus() {
    // 点击文档其他部分关闭所有菜单
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.conversation-menu-trigger') && !e.target.closest('.conversation-menu')) {
            document.querySelectorAll('.conversation-menu.active').forEach(menu => {
                menu.classList.remove('active');
            });
        }
    });

    // 点击三点菜单
    document.querySelectorAll('.conversation-menu-trigger').forEach(trigger => {
        trigger.addEventListener('click', function(e) {
            e.stopPropagation();
            const menuEl = this.nextElementSibling.nextElementSibling.nextElementSibling;

            // 关闭其他菜单
            document.querySelectorAll('.conversation-menu.active').forEach(menu => {
                if (menu !== menuEl) menu.classList.remove('active');
            });

            // 切换当前菜单
            menuEl.classList.toggle('active');
        });
    });

    // 点击重命名选项
    document.querySelectorAll('.rename-conversation').forEach(item => {
        item.addEventListener('click', function(e) {
            e.stopPropagation();
            const conversationItem = this.closest('.conversation-item');
            const titleEl = conversationItem.querySelector('.conversation-title');
            const conversationId = conversationItem.dataset.id;
            const currentTitle = titleEl.textContent;

            // 创建输入框
            const inputEl = document.createElement('input');
            inputEl.type = 'text';
            inputEl.className = 'rename-input';
            inputEl.value = currentTitle;

            // 隐藏标题，显示输入框
            titleEl.style.display = 'none';
            conversationItem.insertBefore(inputEl, titleEl);
            inputEl.focus();

            // 处理输入框失焦和按回车事件
            const handleRename = function() {
                const newTitle = inputEl.value.trim();
                if (newTitle && newTitle !== currentTitle) {
                    // 调用API重命名对话
                    fetch(`/api/conversations/${conversationId}`, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({ title: newTitle })
                    })
                        .then(response => response.json())
                        .then(data => {
                            if (data.success) {
                                titleEl.textContent = newTitle;
                                showNotification('对话重命名成功', 'success');
                            } else {
                                showNotification(data.message || '重命名失败', 'error');
                            }
                        })
                        .catch(error => {
                            console.error('重命名对话错误:', error);
                            showNotification('重命名失败，请稍后重试', 'error');
                        });
                }

                // 移除输入框，显示标题
                titleEl.style.display = '';
                inputEl.remove();
            };

            // 处理输入框失焦事件
            inputEl.addEventListener('blur', handleRename);

            // 处理回车键
            inputEl.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    handleRename();
                }
                // 处理ESC键取消
                else if (e.key === 'Escape') {
                    titleEl.style.display = '';
                    inputEl.remove();
                }
            });

            // 隐藏菜单
            conversationItem.querySelector('.conversation-menu').classList.remove('active');
        });
    });

    // 点击删除选项
    document.querySelectorAll('.delete-conversation').forEach(item => {
        item.addEventListener('click', function(e) {
            e.stopPropagation();
            const conversationItem = this.closest('.conversation-item');
            const conversationId = conversationItem.dataset.id;

            // 确认删除
            if (confirm('确定要删除此对话吗？此操作不可恢复。')) {
                // 调用API删除对话
                fetch(`/api/conversations/${conversationId}`, {
                    method: 'DELETE'
                })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            // 从DOM中删除元素
                            conversationItem.remove();
                            showNotification('对话删除成功', 'success');

                            // 如果当前正在查看的是被删除的对话，则返回到主页
                            if (currentConversationId == conversationId) {
                                document.getElementById('chatOutput').innerHTML = '';
                                showWelcomeMessage();
                                currentConversationId = null;
                            }
                        } else {
                            showNotification(data.message || '删除失败', 'error');
                        }
                    })
                    .catch(error => {
                        console.error('删除对话错误:', error);
                        showNotification('删除失败，请稍后重试', 'error');
                    });
            }

            // 隐藏菜单
            conversationItem.querySelector('.conversation-menu').classList.remove('active');
        });
    });
}

// 加载特定对话的消息
function loadConversationMessages(conversationId) {
    if (!currentUser) return;

    currentConversationId = conversationId;

    fetch(`/api/conversations/${conversationId}/messages`)
        .then(response => {
            if (!response.ok) {
                throw new Error('获取对话消息失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                const chatOutput = document.getElementById('chatOutput');
                chatOutput.innerHTML = '';

                currentMessages = data.messages || [];

                // 显示所有消息
                if (currentMessages.length > 0) {
                    currentMessages.forEach(msg => {
                        // 用户消息
                        const userMsg = document.createElement('div');
                        userMsg.className = 'user-message';
                        userMsg.textContent = msg.user_message;
                        addTimestamp(userMsg, new Date(msg.created_at));
                        chatOutput.appendChild(userMsg);

                        // 机器人回复
                        const botMsg = document.createElement('div');
                        botMsg.className = 'response-message';
                        botMsg.textContent = msg.bot_response;
                        addTimestamp(botMsg, new Date(msg.created_at));
                        chatOutput.appendChild(botMsg);
                    });

                    // 滚动到底部
                    chatOutput.scrollTop = chatOutput.scrollHeight;
                }

                // 如果在移动设备上，加载消息后关闭侧边栏
                if (window.innerWidth <= 768) {
                    document.getElementById('sidebar').classList.add('hidden');
                    document.getElementById('main').classList.add('hidden-sidebar');
                    document.getElementById('toggleIcon').textContent = '☰';
                }
            }
        })
        .catch(error => {
            console.error('加载对话消息失败:', error);
        });
}

// 创建新对话
function createNewConversation() {
    if (!currentUser) {
        // 未登录状态，只清空聊天区域并显示欢迎消息
        showWelcomeMessage();
        return;
    }

    // 已登录状态，创建新对话
    fetch('/api/conversations', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            title: `新对话 ${new Date().toLocaleString('zh-CN', {
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            })}`
        }),
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                currentConversationId = data.conversation_id;

                // 更新对话列表
                loadConversations();

                // 清空聊天区域并显示欢迎消息
                showWelcomeMessage();

                // 将新创建的对话标记为激活状态
                setTimeout(() => {
                    const convItems = document.querySelectorAll('.conversation-item');
                    convItems.forEach(item => {
                        if (item.dataset.id == currentConversationId) {
                            item.classList.add('active');
                        } else {
                            item.classList.remove('active');
                        }
                    });
                }, 100);
            }
        })
        .catch(error => {
            console.error('创建新对话失败:', error);
        });
}

// 格式化时间
function formatTime(date) {
    const now = date || new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
}

// 添加时间戳到消息
function addTimestamp(element, date) {
    const timeSpan = document.createElement('span');
    timeSpan.className = 'message-time';
    timeSpan.style.fontSize = '12px';
    timeSpan.style.color = '#888';
    timeSpan.style.marginTop = '5px';
    timeSpan.style.display = 'block';
    timeSpan.textContent = formatTime(date);
    element.appendChild(timeSpan);
}

// 发送消息
function sendMessage() {
    var message = chatInput.value.trim();
    if (message) {
        // 如果用户已登录但没有当前对话，先创建一个新对话
        if (currentUser && !currentConversationId) {
            // 创建临时UI显示
            var userMessageElement = document.createElement('div');
            userMessageElement.className = 'user-message';
            userMessageElement.textContent = message;
            addTimestamp(userMessageElement);
            chatOutput.appendChild(userMessageElement);

            chatInput.value = '';

            // 显示打字指示器
            var typingIndicator = document.getElementById('typingIndicator');
            typingIndicator.style.display = 'flex';
            chatOutput.scrollTop = chatOutput.scrollHeight;

            // 创建新对话并发送消息
            fetch('/api/conversations', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    title: `关于 ${message.substring(0, 20)}${message.length > 20 ? '...' : ''}`
                }),
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        currentConversationId = data.conversation_id;

                        // 更新对话列表
                        loadConversations();

                        // 发送消息
                        sendMessageToAPI(message, currentConversationId);
                    } else {
                        // 处理错误
                        typingIndicator.style.display = 'none';
                        var errorMsg = document.createElement('div');
                        errorMsg.className = 'response-message error';
                        errorMsg.textContent = '创建对话失败，请稍后重试。';
                        addTimestamp(errorMsg);
                        chatOutput.appendChild(errorMsg);
                        chatOutput.scrollTop = chatOutput.scrollHeight;
                    }
                })
                .catch(error => {
                    console.error('创建对话错误:', error);
                    typingIndicator.style.display = 'none';
                    var errorMsg = document.createElement('div');
                    errorMsg.className = 'response-message error';
                    errorMsg.textContent = '创建对话失败，请稍后重试。';
                    addTimestamp(errorMsg);
                    chatOutput.appendChild(errorMsg);
                    chatOutput.scrollTop = chatOutput.scrollHeight;
                });
        } else {
            // 显示用户消息
            var userMessageElement = document.createElement('div');
            userMessageElement.className = 'user-message';
            userMessageElement.textContent = message;
            addTimestamp(userMessageElement);
            chatOutput.appendChild(userMessageElement);

            chatInput.value = '';

            // 显示打字指示器
            var typingIndicator = document.getElementById('typingIndicator');
            typingIndicator.style.display = 'flex';
            chatOutput.scrollTop = chatOutput.scrollHeight;

            // 发送消息到API
            sendMessageToAPI(message, currentConversationId);
        }
    }
}

// 发送消息到API
function sendMessageToAPI(message, conversationId) {
    // 构建API URL
    let apiUrl = `/get?msg=${encodeURIComponent(message)}`;
    if (conversationId) {
        apiUrl += `&conversation_id=${conversationId}`;
    }

    // 发送请求
    fetch(apiUrl)
        .then(response => response.json())
        .then(data => {
            // 隐藏打字指示器
            var typingIndicator = document.getElementById('typingIndicator');
            typingIndicator.style.display = 'none';

            // 显示回复消息
            var responseMessageElement = document.createElement('div');
            responseMessageElement.className = 'response-message';
            responseMessageElement.textContent = data.response;
            addTimestamp(responseMessageElement);
            chatOutput.appendChild(responseMessageElement);

            chatOutput.scrollTop = chatOutput.scrollHeight;

            // 如果用户已登录，刷新历史记录
            if (currentUser) {
                // 延迟一会儿再加载，确保数据库已更新
                setTimeout(loadConversations, 500);
            }
        })
        .catch(error => {
            var typingIndicator = document.getElementById('typingIndicator');
            typingIndicator.style.display = 'none';
            console.error('获取回复失败:', error);

            // 显示错误消息
            var errorMessageElement = document.createElement('div');
            errorMessageElement.className = 'response-message error';
            errorMessageElement.textContent = '抱歉，发生错误，请稍后重试。';
            addTimestamp(errorMessageElement);
            chatOutput.appendChild(errorMessageElement);

            chatOutput.scrollTop = chatOutput.scrollHeight;
        });
}

// 侧边栏切换
document.getElementById('toggleButton').addEventListener('click', function () {
    var sidebar = document.getElementById('sidebar');
    var main = document.getElementById('main');
    var toggleIcon = document.getElementById('toggleIcon');

    sidebar.classList.toggle('hidden');
    main.classList.toggle('hidden-sidebar');

    // 更改图标
    if (sidebar.classList.contains('hidden')) {
        toggleIcon.textContent = '☰';
    } else {
        toggleIcon.textContent = '✖';
    }
});

// 新对话按钮
document.getElementById('newConversationButton').addEventListener('click', function() {
    // 清空当前对话ID，这样在用户发送新消息时会创建新对话
    currentConversationId = null;

    // 创建新对话
    createNewConversation();
});

// 添加事件监听器
document.addEventListener('DOMContentLoaded', function() {
    var chatInput = document.getElementById('chatInput');
    var sendButton = document.getElementById('sendButton');
    var chatOutput = document.getElementById('chatOutput');

    sendButton.addEventListener('click', sendMessage);

    chatInput.addEventListener('keypress', function(event) {
        if (event.key === 'Enter') {
            sendMessage();
        }
    });
});