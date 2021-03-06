package com.starlley.cursomc.services;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starlley.cursomc.domain.ItemPedido;
import com.starlley.cursomc.domain.PagamentoComBoleto;
import com.starlley.cursomc.domain.Pedido;
import com.starlley.cursomc.domain.enums.EstadoPagamento;
import com.starlley.cursomc.repositories.ItemPedidoRepository;
import com.starlley.cursomc.repositories.PagamentoRepository;
import com.starlley.cursomc.repositories.PedidoRepository;
import com.starlley.cursomc.services.exceptions.ObjetcNotFoundException;

@Service
public class PedidoService {

	// Buscando uma categoria por codigo //

	// Declarando uma dependencia //
	@Autowired
	private PedidoRepository repo;

	@Autowired
	private BoletoService boletoService;

	@Autowired
	private PagamentoRepository pagamentoRepository;

	@Autowired
	private ProdutoService produtoService;

	@Autowired
	private ItemPedidoRepository itemPedidoRepository;

	@Autowired
	private ClienteService clienteService;

	@Autowired
	private EmailService emailService;

	// Criando um metodo de busca //
	public Pedido find(Integer id) {

		Optional<Pedido> obj = repo.findById(id);

		return obj.orElseThrow(() -> new ObjetcNotFoundException(
				"Objeto não encontrado! Id: " + id + ", Tipo: " + Pedido.class.getName()));

	}

	@Transactional
	public Pedido insert(Pedido obj) {

		obj.setId(null);
		obj.setInstante(new Date());
		obj.setCliente(clienteService.find(obj.getCliente().getId()));
		obj.getPagamento().setEstado(EstadoPagamento.PENDENTE);
		obj.getPagamento().setPedido(obj);
		if (obj.getPagamento() instanceof PagamentoComBoleto) {

			PagamentoComBoleto pagto = (PagamentoComBoleto) obj.getPagamento();
			boletoService.preencherPagamentoComBoleto(pagto, obj.getInstante());

		}
		// Salvando o pedido e o pagamento //
		obj = repo.save(obj);
		pagamentoRepository.save(obj.getPagamento());

		for (ItemPedido ip : obj.getItens()) {
			ip.setDesconto(0.0);
			ip.setProduto(produtoService.find(ip.getProduto().getId()));
			ip.setPreco(ip.getProduto().getPreco());
			ip.setPedido(obj);

		}

		itemPedidoRepository.saveAll(obj.getItens());
		//System.out.println(obj);
		emailService.sendOrderConfirmationEmail(obj);
		return obj;
	}

}
