function logHist(data)

[n, xout] = hist(data, sqrt(length(data)));
bar(xout, n, 'barwidth', 1, 'basevalue', 1);
set(gca,'YScale','log');
end
