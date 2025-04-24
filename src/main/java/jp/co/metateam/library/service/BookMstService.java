package jp.co.metateam.library.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public BookMstService(BookMstRepository bookMstRepository){
        this.bookMstRepository = bookMstRepository;
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
    //今回のポスト //バリデーションの処理
    @PostMapping("/book/add")
    public boolean CheckBook (BookMstDto bookMstDto,Model model){
        //bookmstの内容を定義
        String bookTitle = bookMstDto.getTitle();
        String bookIsbn = bookMstDto.getIsbn();
        //エラーを貯めるためリスト化する　
        List<String> errTitleList = new ArrayList<>();
        List<String> errIsbnList = new ArrayList<>();
        //書籍名の必須

        if (StringUtils.isEmpty(bookTitle)) {
            errTitleList.add("書籍名は必須です");
            model.addAttribute("errTitle",errTitleList); //controllerからHTMLテンプレートに値を渡す
            //書籍名文字数
        }if (bookTitle.length() > 255 ){
            errTitleList.add("書籍名は255文字以内で入力してください");
            model.addAttribute("errTitle",errTitleList);
        }
        //ISBNの必須
        if (StringUtils.isEmpty(bookIsbn)) {
            errIsbnList.add("ISBNは必須です");
            model.addAttribute("errIsbn",errIsbnList);
            return !errIsbnList.isEmpty()|| !errIsbnList.isEmpty();
        }
        //ISBNの桁数
        if (bookIsbn.length() != 13 ){
            errIsbnList.add("ISBNは13桁で入力してください");
            model.addAttribute("errTitle",errIsbnList);
        }
        //ISBNの半角数字
        if (!bookIsbn.matches("^[0-9]+$")) {
            errIsbnList.add("ISBNは半角数字で入力してください");
            model.addAttribute("errIsbn", errIsbnList);
        }
        if(!errIsbnList.isEmpty() || !errTitleList.isEmpty()){
        return true;
        }
        return false;
    }
    public Boolean checkIsbnEntry(BookMstDto bookMstDto,Model model) {
        String getIsbn = bookMstDto.getIsbn();
        List<String>errTitleList = new ArrayList<>() ;
        List<String>errIsbnList = new ArrayList<>() ;
        List<BookMst> bookMst  = this.bookMstRepository.selectByIsbn(getIsbn);
        if (!bookMst.isEmpty()) {
            errIsbnList.add("登録されているISBNです");
            model.addAttribute("errIsbn", errIsbnList);
            return true;
        }
        return false;
    }
}