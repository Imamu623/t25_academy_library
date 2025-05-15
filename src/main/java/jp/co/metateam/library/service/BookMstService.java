package jp.co.metateam.library.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.yaml.snakeyaml.events.Event.ID;

import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.AccountDto;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.BookMstDto;
import jp.co.metateam.library.repository.BookMstRepository;

@Service
public class BookMstService {

    private final BookMstRepository bookMstRepository;

    @Autowired
    public BookMstService(BookMstRepository bookMstRepository) {
        this.bookMstRepository = bookMstRepository;
    }

    public BookMstDto findBookId(Long id) {
    Optional<BookMst> optionalBook = bookMstRepository.findById(id);
    if (!optionalBook.isPresent()) {
        return null;
    }

    BookMst book = optionalBook.get();
    BookMstDto dto = new BookMstDto();
    dto.setTitle(book.getTitle());
    dto.setIsbn(book.getIsbn());
    dto.setId(book.getId());

    return dto;
}

    // BookMstService.java

public Optional<BookMst> findById(Long id) {
    return bookMstRepository.findById(id);
}

    public List<BookMstDto> findAvailableWithStockCount() {
        List<BookMst> books = this.bookMstRepository.findLimitedBook();
        List<BookMstDto> bookMstDtoList = new ArrayList<BookMstDto>();

        // 書籍の在庫数を取得
        // FIXME: 現状は書籍ID毎にDBに問い合わせている。一度のSQLで完了させたい。
        for (int i = 0; i < books.size(); i++) {
            BookMst book = books.get(i);
            BookMstDto bookMstDto = new BookMstDto();
            bookMstDto.setId(book.getId());
            bookMstDto.setIsbn(book.getIsbn());
            bookMstDto.setTitle(book.getTitle());
            bookMstDtoList.add(bookMstDto);
        }

        return bookMstDtoList;
    }

    @Transactional
    public void save(BookMstDto bookMstDto) {
        try {
            // BookMstDtoからbookmstdtoへの変換
            BookMst bookMst = new BookMst();

            bookMst.setTitle(bookMstDto.getTitle());
            bookMst.setIsbn(bookMstDto.getIsbn());

            // データベースへの保存
            this.bookMstRepository.save(bookMst);
        } catch (Exception e) {
            throw e;
        }
    }

    // 今回のポスト //バリデーションの処理
    @Transactional
    public boolean CheckBook(BookMstDto bookMstDto, Model model) {
        // bookmstの内容を定義
        String bookTitle = bookMstDto.getTitle();
        String bookIsbn = bookMstDto.getIsbn();
        // エラーを貯めるためリスト化する
        List<String> errTitleList = new ArrayList<>();
        List<String> errIsbnList = new ArrayList<>();
        // 書籍名の必須
        if (StringUtils.isEmpty(bookTitle)) {
            errTitleList.add("書籍名は必須です");
            model.addAttribute("errTitle", errTitleList); // controllerからHTMLテンプレートに値を渡す
            // 書籍名文字数
        }
        if (bookTitle.length() > 255) {
            errTitleList.add("書籍名は255文字以内で入力してください");
            model.addAttribute("errTitle", errTitleList);
        }
        // ISBNの必須
        if (StringUtils.isEmpty(bookIsbn)) {
            errIsbnList.add("ISBNは必須です");
            model.addAttribute("errIsbn", errIsbnList);
            return !errIsbnList.isEmpty() || !errTitleList.isEmpty();
        }
        // ISBNの桁数
        if (bookIsbn.length() != 13) {
            errIsbnList.add("ISBNは13桁で入力してください");
            model.addAttribute("errIsbn", errIsbnList);
        }
        // ISBNの半角数字
        if (!bookIsbn.matches("^[0-9]+$")) {
            errIsbnList.add("ISBNは半角数字で入力してください");
            model.addAttribute("errIsbn", errIsbnList);
        }
        if (!errIsbnList.isEmpty() || !errTitleList.isEmpty()) {
            return true;
        }
        return false;
    }

    public Boolean checkIsbnEntry(BookMstDto bookMstDto, Model model) {
        String getIsbn = bookMstDto.getIsbn();
        List<String> errTitleList = new ArrayList<>();
        List<String> errIsbnList = new ArrayList<>();
        List<BookMst> bookMst = this.bookMstRepository.selectByIsbn(getIsbn);
        if (!bookMst.isEmpty()) {
            errIsbnList.add("登録されているISBNです");
            model.addAttribute("errIsbn", errIsbnList);
            return true;
        }
        return false;
    }

    // 書籍名、ISBNの変更あり,変更なしかと書籍情報が削除されていないか確認したい
   public Boolean update(BookMstDto bookMstDto, Model model) {
    // 書籍情報の存在チェック
    Optional<BookMst> optionalBook = bookMstRepository.findById(bookMstDto.getId());
    if (optionalBook.isEmpty()) {
        model.addAttribute("errorMessage", "対象の書籍が削除されています。");
        return false;
    }

    BookMst book = optionalBook.get();
    boolean changed = false;

    // 書籍名の変更チェック
    if (!Objects.equals(book.getTitle(), bookMstDto.getTitle())) {
        book.setTitle(bookMstDto.getTitle());
        changed = true;
    }

    // ISBNの変更チェック（自分以外との重複をチェック）
    if (!Objects.equals(book.getIsbn(), bookMstDto.getIsbn())) {
        List<BookMst> duplicates = bookMstRepository.selectByIsbn(bookMstDto.getIsbn());
        if (duplicates.stream().anyMatch(b -> !b.getId().equals(book.getId()))) {
            // ISBNが重複している場合
            List<String> errIsbnList = new ArrayList<>();
            errIsbnList.add("入力されたISBNはすでに別の書籍に登録されています。");
            model.addAttribute("errIsbn", errIsbnList);
            return false;  // ISBNが重複しているので、処理を終了
        }
        book.setIsbn(bookMstDto.getIsbn());
        changed = true;
    }

    // 変更があった場合は保存、なければ何もしない
    if (changed) {
        bookMstRepository.save(book);
        return true;  // 変更があった場合はtrueを返す
    } else {
        // 変更がなければfalseを返す
        model.addAttribute("noChange", true);
        model.addAttribute("infoMessage", "変更内容がありません。");
        return false;
    }
}
}
