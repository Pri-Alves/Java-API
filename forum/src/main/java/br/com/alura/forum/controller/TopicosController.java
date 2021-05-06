package br.com.alura.forum.controller;

import br.com.alura.forum.controller.dto.DetalhesDoTopicoDto;
import br.com.alura.forum.controller.dto.TopicoDto;
import br.com.alura.forum.controller.form.AtualizacaoTopicosForm;
import br.com.alura.forum.controller.form.TopicosForm;
import br.com.alura.forum.modelo.Topico;
import br.com.alura.forum.repository.CursoRepository;
import br.com.alura.forum.repository.TopicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/topicos")
public class TopicosController {

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private TopicoRepository topicoRepository;

    @GetMapping
    @Cacheable(value = "listaDeTopicos") //Criar cache para melhorrar a performance
    // utilizando RestController no lugar de Controlle, elimina a necessidade de colocar o @ResponseBody // usado para quando nao vamos navegar para uma pagina e sim fazer uma api rest
    public Page<TopicoDto> lista(@RequestParam(required = false) String nomeCurso,
                                 @PageableDefault(sort = "id", direction = Sort.Direction.ASC, page = 0, size = 10) Pageable paginacao){
        //Default: id em ordem crescente, pagina 0 com 10 registros
        //Exemplo de utilização dos filtros /topicos?page=0&size=10&sort=dataCriacao,desc&sort=mensagem,asc

        if (nomeCurso == null){
            Page<Topico> topicos = topicoRepository.findAll(paginacao); //findAll vem da herança do jpa em topicosrepository e serve para consultar tudo
            return TopicoDto.converter(topicos);
        } else {
            Page<Topico> topicos = topicoRepository.findByCursoNome(nomeCurso, paginacao); //criar metodo que busca o nome na tabela curso, ele entende através do proprio nome, poderia ser tb findByCurso_Nome
            return TopicoDto.converter(topicos);
        }
    }

    @PostMapping
    @Transactional
    @CacheEvict(value = "listaDeTopicos", allEntries = true) //apagar o cache
    public ResponseEntity<TopicoDto> cadastrar(@RequestBody @Valid TopicosForm form, UriComponentsBuilder uriBuilder){
        Topico topico = form.converter(cursoRepository);
        topicoRepository.save(topico);

        URI uri = uriBuilder.path("/topicos/{id}").buildAndExpand(topico.getId()).toUri();
        return ResponseEntity.created(uri).body(new TopicoDto(topico));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalhesDoTopicoDto> detalhar(@PathVariable Long id) { // Path é necessario para que o Spring entenda que o id vira após o / e nao apos o ?
        // poderia usar o topicoDto, porem ele só tras 4 informações, para isso foi criado o DetalhesDoTopicoDto
        Optional<Topico> topico = topicoRepository.findById(id);//getOne é um metodo pronto para buscar por id, porem ele realiza exception, foi alterado para findById
        if (topico.isPresent()){
            return ResponseEntity.ok(new DetalhesDoTopicoDto(topico.get()));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    @CacheEvict(value = "listaDeTopicos", allEntries = true) //apagar o cache
    @Transactional //pra ele realizar a transação automaticmente caso nao ocorram exceptions
    public ResponseEntity<TopicoDto> atualizar(@PathVariable Long id, @RequestBody @Valid AtualizacaoTopicosForm form ) {
        Optional<Topico> optional = topicoRepository.findById(id);
        if (optional.isPresent()){
            Topico topico = form.atualizar(id, topicoRepository);
            return ResponseEntity.ok(new TopicoDto(topico));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    @CacheEvict(value = "listaDeTopicos", allEntries = true) //apagar o cache
    public ResponseEntity<TopicoDto> remover(@PathVariable Long id) {
        Optional<Topico> optional = topicoRepository.findById(id);
        if (optional.isPresent()){
            topicoRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
