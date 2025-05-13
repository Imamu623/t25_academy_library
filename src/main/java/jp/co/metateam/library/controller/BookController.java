package jp.co.metateam.library.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.AccountDto;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.BookMstDto;
import jp.co.metateam.library.repository.BookMstRepository;
import jp.co.metateam.library.service.BookMstService;
import lombok.extern.log4j.Log4j2;

/**
 * 書籍関連クラス
 */
@Log4j2
@Controller
public class BookController {

    private final BookMstService bookMstService;

    @Autowired
    public BookController(BookMstService bookMstService) {
        this.bookMstService = bookMstService;
    }

    @GetMapping("/book/index")
    public String index(Model model) {
        // 書籍を全件取得
        List<BookMstDto> bookMstList = this.bookMstService.findAvailableWithStockCount();

        model.addAttribute("bookMstList", bookMstList);

        return "book/index";
    }

    @GetMapping("/book/add")
    public String add(Model model) {
        if (!model.containsAttribute("bookMstDto")) {
            model.addAttribute("bookMstDto", new BookMstDto());
        }

        return "book/add";
    }

    // 今回のポスト
    @PostMapping("/book/add")
    public String register(@ModelAttribute("bookMstDto") BookMstDto bookMstDto, BindingResult result, Model model) {
        try {
            boolean checkResult = bookMstService.CheckBook(bookMstDto, model);

            if (checkResult) {
                return "book/add";
            }
            boolean checkIsbnResult = bookMstService.checkIsbnEntry(bookMstDto, model);
            if (checkIsbnResult) {
                return "book/add";
            }
            // 登録処理
            bookMstService.save(bookMstDto);
            return "redirect:/book/index";

        } catch (Exception e) {
            log.error(e.getMessage());
            return "book/index";
        }
    }

    // 書籍一覧編集機能追加
    @GetMapping("/book/edit")
    public String edit(@RequestParam("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            BookMstDto book = bookMstService.findBookId(id); // 本を探す
            if (book == null) {
                // 書籍が削除されていたなどで見つからない場合
                redirectAttributes.addFlashAttribute("errorMessage", "対象の書籍が削除されています。");
                return "redirect:/book/index";
            }
            model.addAttribute("title", "書籍編集");
            model.addAttribute("bookMstDto", book);
            return "book/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "対象の書籍が削除されています。");
            return "redirect:/book/index";
        }
    }

    @PostMapping("/book/edit")
    public String update(@ModelAttribute("bookMstDto") BookMstDto bookMstDto, BindingResult result, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            boolean checkResult = bookMstService.CheckBook(bookMstDto, model);
            // 書籍の存在チェックで書籍情報がなかった場合にエラーメッセージを表示する
            Optional<BookMst> optionalBook = bookMstService.findById(bookMstDto.getId());
            if (optionalBook.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "対象の書籍が削除されています");
                return "redirect:/book/index";
            }
            // 入力値のバリデーションチェック呼び出し
            if (checkResult) {
                return "book/edit"; // エラーメッセージがある場合は編集画面に戻す
            }
            // 更新処理
            boolean update = bookMstService.update(bookMstDto, model);
            if (!update) {
                // どちらも変更がなかった場合にエラーメッセージを表示する
                if (!model.containsAttribute("errIsbn")) {
                    List<String> errList = new ArrayList<>();
                    errList.add("書籍名、ISBNの変更がありません。");
                    model.addAttribute("errUpdate", errList);
                }
                return "book/edit";
            }
            return "redirect:/book/index";

        } catch (Exception e) {
            log.error(e.getMessage());

            return "book/edit";
        }
    }
}
