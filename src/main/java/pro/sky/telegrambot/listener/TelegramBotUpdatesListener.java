package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.Entity.NotificationTask;
import pro.sky.telegrambot.Repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private static final Pattern PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository repository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            String text = update.message().text();
            Long chatId = update.message().chat().id();
            Matcher matcher = PATTERN.matcher(text);  //проверяем текст на соответствие с pattern
            if("/start".equalsIgnoreCase(text)) {
                telegramBot.execute(new SendMessage(chatId, "Привет"));
            } else if (matcher.matches()) {
                try {
                    String time = matcher.group(1);//1 группа - время
                    String item = matcher.group(3);
                    LocalDateTime date = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                    NotificationTask task = new NotificationTask();
                    task.setChatId(chatId);
                    task.setText(item);
                    task.setExecDate(date);
                    repository.save(task);
                } catch (DateTimeParseException e) {
                    telegramBot.execute(new SendMessage(chatId, "неверный формат даты"));
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Scheduled(fixedDelay = 60_000L)
    public void schedule() {
        List<NotificationTask> tasks = repository.findAllByExecDate(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        tasks.forEach(t -> {
            SendResponse response = telegramBot.execute(new SendMessage(t.getChatId(), t.getText()));
            if(response.isOk()) {
                repository.delete(t);
            }
        });
    }
}
