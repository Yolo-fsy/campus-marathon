/* AI 知识库：上传区文件选择 + 对话页 SSE 流式渲染 */
(function () {
    'use strict';

    var ctx = window.KB_CTX || '';

    /* ---------------- 上传区 ---------------- */
    var fileInput = document.getElementById('kbFile');
    if (fileInput) {
        var nameLabel = document.getElementById('kbFileName');
        var dropZone = document.getElementById('kbDrop');

        var syncName = function () {
            if (!nameLabel) {
                return;
            }
            nameLabel.textContent = fileInput.files && fileInput.files.length
                ? fileInput.files[0].name
                : '尚未选择文件';
        };
        fileInput.addEventListener('change', syncName);

        if (dropZone) {
            ['dragenter', 'dragover'].forEach(function (type) {
                dropZone.addEventListener(type, function (event) {
                    event.preventDefault();
                    dropZone.classList.add('is-drag');
                });
            });
            ['dragleave', 'drop'].forEach(function (type) {
                dropZone.addEventListener(type, function (event) {
                    event.preventDefault();
                    dropZone.classList.remove('is-drag');
                });
            });
            dropZone.addEventListener('drop', function (event) {
                if (event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files.length) {
                    fileInput.files = event.dataTransfer.files;
                    syncName();
                }
            });
        }
    }

    /* ---------------- 对话页 ---------------- */
    var chatForm = document.getElementById('chatForm');
    if (!chatForm) {
        return;
    }

    var chatLog = document.getElementById('chatLog');
    var chatInput = document.getElementById('chatInput');
    var chatSend = document.getElementById('chatSend');
    var chatMode = document.getElementById('chatMode');
    var chatDocId = document.getElementById('chatDocId');
    var busy = false;

    chatForm.addEventListener('submit', function (event) {
        event.preventDefault();
        ask();
    });

    if (chatInput) {
        chatInput.addEventListener('keydown', function (event) {
            if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
                event.preventDefault();
                ask();
            }
        });
    }

    function ask() {
        if (busy) {
            return;
        }
        var question = chatInput.value.trim();
        if (!question) {
            chatInput.focus();
            return;
        }
        if (!chatLog) {
            return;
        }

        appendBubble('is-user', question);
        chatInput.value = '';
        var bot = appendBubble('is-bot is-typing', '');
        var citations = document.createElement('div');
        citations.className = 'citations';

        busy = true;
        chatSend.disabled = true;
        chatSend.textContent = '生成中';

        fetch(ctx + '/ai/ask', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
            body: 'question=' + encodeURIComponent(question)
                + '&docId=' + encodeURIComponent(chatDocId ? chatDocId.value : '')
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('请求失败（HTTP ' + response.status + '）');
            }
            if (!response.body || !response.body.getReader) {
                return response.text().then(function (text) {
                    processBuffer(text + '\n\n', bot, citations, true);
                });
            }
            var reader = response.body.getReader();
            var decoder = new TextDecoder('utf-8');
            var buffer = '';

            function pump() {
                return reader.read().then(function (result) {
                    if (result.done) {
                        buffer += decoder.decode();
                        if (buffer.trim()) {
                            processBuffer(buffer + '\n\n', bot, citations, false);
                        }
                        return;
                    }
                    buffer += decoder.decode(result.value, {stream: true});
                    buffer = processBuffer(buffer, bot, citations, false);
                    return pump();
                });
            }

            return pump();
        }).catch(function (error) {
            bot.classList.remove('is-typing');
            bot.classList.add('is-error');
            bot.textContent = (bot.textContent || '') + '问答失败：' + error.message + '，请重试。';
            scrollToBottom();
        }).finally(function () {
            busy = false;
            chatSend.disabled = false;
            chatSend.textContent = '发送';
            bot.classList.remove('is-typing');
            scrollToBottom();
        });
    }

    function appendBubble(className, text) {
        var bubble = document.createElement('div');
        bubble.className = 'chat-bubble ' + className;
        bubble.textContent = text;
        chatLog.appendChild(bubble);
        scrollToBottom();
        return bubble;
    }

    function processBuffer(buffer, bot, citations, flush) {
        var parts = buffer.split('\n\n');
        var remainder = parts.pop();
        for (var i = 0; i < parts.length; i++) {
            handleBlock(parts[i], bot, citations);
        }
        if (flush && remainder.trim()) {
            handleBlock(remainder, bot, citations);
            remainder = '';
        }
        return remainder;
    }

    function handleBlock(block, bot, citations) {
        var event = 'message';
        var dataLines = [];
        block.split('\n').forEach(function (line) {
            if (line.indexOf('event:') === 0) {
                event = line.slice(6).trim();
            } else if (line.indexOf('data:') === 0) {
                dataLines.push(line.slice(5).replace(/^\s/, ''));
            }
        });
        if (!dataLines.length) {
            return;
        }
        var data = dataLines.join('\n');
        if (!data) {
            return;
        }

        if (event === 'meta') {
            showMode(data);
            return;
        }
        if (event === 'citations') {
            renderCitations(data, citations);
            return;
        }
        if (event === 'error') {
            var error = parseJson(data);
            bot.classList.remove('is-typing');
            bot.classList.add('is-error');
            bot.textContent = (bot.textContent || '') + (error && error.message ? error.message : '问答服务异常');
            scrollToBottom();
            return;
        }
        if (data === '[DONE]') {
            return;
        }
        var payload = parseJson(data);
        if (payload && payload.t) {
            bot.textContent += payload.t;
            scrollToBottom();
        }
    }

    function showMode(data) {
        if (!chatMode) {
            return;
        }
        var meta = parseJson(data);
        if (!meta) {
            return;
        }
        if (meta.mode === 'llm') {
            chatMode.innerHTML = '模式：<strong>大模型在线回答</strong> · ' + escapeHtml(meta.model || '');
        } else {
            chatMode.innerHTML = '模式：<strong>检索演示</strong> · 未配置模型密钥，仅展示检索片段';
        }
    }

    function renderCitations(data, container) {
        var payload = parseJson(data);
        if (!payload || !payload.items || !payload.items.length || !chatLog) {
            return;
        }
        container.innerHTML = '';
        var title = document.createElement('div');
        title.className = 'citations-title';
        title.textContent = '引用片段';
        container.appendChild(title);

        payload.items.forEach(function (item) {
            var card = document.createElement('div');
            card.className = 'citation-card';

            var head = document.createElement('strong');
            head.textContent = '[' + item.index + '] 《' + (item.title || '未命名文档') + '》第 ' + item.seq + ' 段';

            var meta = document.createElement('small');
            meta.textContent = '相关度 ' + item.score + ' · 文档 #' + item.docId;

            var preview = document.createElement('span');
            preview.textContent = item.preview || '';

            card.appendChild(head);
            card.appendChild(meta);
            card.appendChild(preview);
            container.appendChild(card);
        });
        chatLog.appendChild(container);
        scrollToBottom();
    }

    function parseJson(text) {
        try {
            return JSON.parse(text);
        } catch (error) {
            return null;
        }
    }

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function scrollToBottom() {
        if (chatLog) {
            chatLog.scrollTop = chatLog.scrollHeight;
        }
    }
})();
