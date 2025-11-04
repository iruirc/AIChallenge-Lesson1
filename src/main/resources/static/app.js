// Конфигурация
const API_URL = '/chat';
const REQUEST_TIMEOUT = 30000; // 30 секунд

// DOM элементы
const messagesContainer = document.getElementById('messagesContainer');
const messageInput = document.getElementById('messageInput');
const sendButton = document.getElementById('sendButton');
const statusElement = document.getElementById('status');

// Состояние
let isLoading = false;

// Инициализация
document.addEventListener('DOMContentLoaded', () => {
    // Обработчик отправки по клику
    sendButton.addEventListener('click', handleSendMessage);

    // Обработчик отправки по Enter (Shift+Enter для новой строки)
    messageInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    });

    // Автоматическое изменение высоты textarea
    messageInput.addEventListener('input', () => {
        messageInput.style.height = 'auto';
        messageInput.style.height = messageInput.scrollHeight + 'px';
    });
});

// Основная функция отправки сообщения
async function handleSendMessage() {
    const message = messageInput.value.trim();

    if (!message || isLoading) {
        return;
    }

    // Очищаем поле ввода
    messageInput.value = '';
    messageInput.style.height = 'auto';

    // Удаляем приветственное сообщение при первом запросе
    const welcomeMessage = document.querySelector('.welcome-message');
    if (welcomeMessage) {
        welcomeMessage.remove();
    }

    // Отображаем сообщение пользователя
    addMessage(message, 'user');

    // Показываем индикатор загрузки
    const loadingMessageId = addLoadingMessage();

    // Блокируем интерфейс
    setLoading(true);
    updateStatus('Отправка запроса...');

    try {
        // Отправляем запрос к API с таймаутом
        const response = await fetchWithTimeout(API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ message }),
        }, REQUEST_TIMEOUT);

        // Удаляем индикатор загрузки
        removeLoadingMessage(loadingMessageId);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        // Проверяем наличие ответа
        if (data.response) {
            addMessage(data.response, 'assistant');
            updateStatus('');
        } else {
            throw new Error('Пустой ответ от сервера');
        }

    } catch (error) {
        console.error('Error:', error);

        // Удаляем индикатор загрузки
        removeLoadingMessage(loadingMessageId);

        // Определяем тип ошибки
        let errorMessage = 'Что-то пошло не так';

        if (error.name === 'AbortError') {
            errorMessage = 'Что-то пошло не так (превышено время ожидания)';
        } else if (error.message.includes('Failed to fetch')) {
            errorMessage = 'Что-то пошло не так (нет соединения с сервером)';
        }

        // Отображаем ошибку
        addMessage(errorMessage, 'error');
        updateStatus(errorMessage, 'error');

        // Очищаем статус через 3 секунды
        setTimeout(() => updateStatus(''), 3000);
    } finally {
        setLoading(false);
    }
}

// Добавление сообщения в чат
function addMessage(text, type) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = text;

    messageDiv.appendChild(contentDiv);
    messagesContainer.appendChild(messageDiv);

    // Прокрутка вниз
    scrollToBottom();
}

// Добавление индикатора загрузки
function addLoadingMessage() {
    const loadingId = `loading-${Date.now()}`;
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message assistant loading';
    messageDiv.id = loadingId;

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';

    const typingIndicator = document.createElement('div');
    typingIndicator.className = 'typing-indicator';
    typingIndicator.innerHTML = '<span></span><span></span><span></span>';

    contentDiv.appendChild(typingIndicator);
    messageDiv.appendChild(contentDiv);
    messagesContainer.appendChild(messageDiv);

    scrollToBottom();

    return loadingId;
}

// Удаление индикатора загрузки
function removeLoadingMessage(loadingId) {
    const loadingMessage = document.getElementById(loadingId);
    if (loadingMessage) {
        loadingMessage.remove();
    }
}

// Прокрутка к последнему сообщению
function scrollToBottom() {
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// Обновление статуса
function updateStatus(text, type = '') {
    statusElement.textContent = text;
    statusElement.className = `status ${type}`;
}

// Блокировка/разблокировка интерфейса
function setLoading(loading) {
    isLoading = loading;
    sendButton.disabled = loading;
    messageInput.disabled = loading;
}

// Fetch с таймаутом
function fetchWithTimeout(url, options, timeout) {
    return Promise.race([
        fetch(url, options),
        new Promise((_, reject) =>
            setTimeout(() => reject(new Error('AbortError')), timeout)
        )
    ]);
}
