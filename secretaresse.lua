hs.alert('Secretaresse app started')

local secretaresseMenubar = hs.menubar.new()

secretaresseMenubar:setTitle('S')
secretaresseMenubar:setTooltip('Secretaresse - Not scheduled')

secretaresseMenubar:setMenu({
  {title = "Run now", fn = function() runSecretaresse() end},
  {title = "Schedule every 2 hours", fn = function() startTimer(120) end},
  {title = "Turn off", fn = function() stopTimer() end}
})

function stopTimer()
  if timer and timer:running() then
    timer:stop()
    secretaresseMenubar:setTooltip('Secretaresse - Not scheduled')
  end
end

function startTimer(minutes)
  stopTimer()
  local seconds = hs.timer.minutes(minutes)
  timer = hs.timer.doEvery(seconds, function() runSecretaresse() end)
  secretaresseMenubar:setTooltip('Secretaresse - Running every ' .. minutes .. ' minutes')
end

function runSecretaresse()
  if secretaresseDir then
    hs.execute('cd ' .. secretaresseDir .. ' && java -jar secretaresse.jar', true)
  else
    hs.alert('Do not forget to add the global variable secretaresseDir with the dir where the project lives.')
  end
end

startTimer(120)

